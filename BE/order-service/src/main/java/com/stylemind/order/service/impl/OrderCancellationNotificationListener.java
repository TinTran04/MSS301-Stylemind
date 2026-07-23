package com.stylemind.order.service.impl;

import com.stylemind.order.entity.OrderCancellation;
import com.stylemind.order.event.OrderCancellationApprovedEvent;
import com.stylemind.order.event.OrderCancellationRejectedEvent;
import com.stylemind.order.event.OrderCancellationRequestedEvent;
import com.stylemind.order.event.RefundCompletedEvent;
import com.stylemind.order.feign.NotificationClient;
import com.stylemind.order.feign.PaymentClient;
import com.stylemind.order.feign.UserClient;
import com.stylemind.order.repository.OrderCancellationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCancellationNotificationListener {

    private static final int MAX_ATTEMPTS = 3;

    private final UserClient userClient;
    private final NotificationClient notificationClient;
    private final PaymentClient paymentClient;
    private final OrderCancellationRepository cancellationRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCancellationRequested(OrderCancellationRequestedEvent event) {
        sendCancellationNotification(event.userId(), event.orderId(), event.cancellationId(), "ORDER_CANCELLATION_REQUESTED");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCancellationApproved(OrderCancellationApprovedEvent event) {
        sendCancellationNotification(event.userId(), event.orderId(), event.cancellationId(), "ORDER_CANCELLATION_APPROVED");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCancellationRejected(OrderCancellationRejectedEvent event) {
        sendCancellationNotification(event.userId(), event.orderId(), event.cancellationId(), "ORDER_CANCELLATION_REJECTED");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRefundCompleted(RefundCompletedEvent event) {
        sendRefundCompletedNotification(event.userId(), event.orderId(), event.refundId());
    }

    private void sendCancellationNotification(String userId, String orderId, String cancellationId, String type) {
        OrderCancellation cancellation = cancellationRepository.findById(cancellationId).orElse(null);
        String email = resolveUserEmail(userId);
        if (!StringUtils.hasText(email)) {
            log.warn("No email on file for user {} - skipping {} notification for order {}", userId, type, orderId);
            return;
        }

        String orderRef = formatOrderReference(orderId);
        String title;
        String content;
        if ("ORDER_CANCELLATION_REJECTED".equals(type)) {
            title = "Yêu cầu hủy đơn đã bị từ chối";
            content = buildRejectedContent(orderRef, cancellation);
        } else if ("ORDER_CANCELLATION_APPROVED".equals(type)) {
            title = "Đơn hàng đã được hủy";
            content = buildApprovedContent(orderRef, cancellation);
        } else {
            title = "Yêu cầu hủy đơn đang chờ duyệt";
            content = buildRequestedContent(orderRef, cancellation);
        }

        sendEmailWithRetry(userId, email, type, title, content, null);
    }

    private void sendRefundCompletedNotification(String userId, String orderId, String refundId) {
        String email = resolveUserEmail(userId);
        if (!StringUtils.hasText(email)) {
            log.warn("No email on file for user {} - skipping refund notification for order {}", userId, orderId);
            return;
        }

        PaymentClient.RefundResponse refund = resolveRefund(orderId).orElse(null);
        String orderRef = formatOrderReference(orderId);
        String title = "Hoàn tiền đã hoàn tất";
        String content = buildRefundCompletedContent(orderRef, refund, refundId);
        sendEmailWithRetry(userId, email, "ORDER_REFUND_COMPLETED", title, content, null);
    }

    private String buildRequestedContent(String orderRef, OrderCancellation cancellation) {
        String reason = cancellation != null ? formatCancellationReason(cancellation.getReasonCode()) : "Chưa có lý do";
        String note = cancellation != null ? cancellation.getCustomerNote() : null;
        String requestedAt = cancellation != null ? formatInstant(cancellation.getRequestedAt()) : null;
        return "Yêu cầu hủy cho đơn hàng " + orderRef + " đã được ghi nhận và đang chờ duyệt."
                + " Lý do: " + reason + "."
                + (StringUtils.hasText(note) ? " Ghi chú: " + note + "." : "")
                + (StringUtils.hasText(requestedAt) ? " Thời gian gửi: " + requestedAt + "." : "");
    }

    private String buildApprovedContent(String orderRef, OrderCancellation cancellation) {
        String reason = cancellation != null ? formatCancellationReason(cancellation.getReasonCode()) : "Chưa có lý do";
        String approvedAt = cancellation != null ? formatInstant(cancellation.getApprovedAt()) : null;
        return "Đơn hàng " + orderRef + " đã được hủy."
                + " Lý do xử lý: " + reason + "."
                + (StringUtils.hasText(approvedAt) ? " Thời gian xử lý: " + approvedAt + "." : "")
                + " Nếu đơn đã thanh toán, hệ thống sẽ tiếp tục xử lý hoàn tiền theo quy trình.";
    }

    private String buildRejectedContent(String orderRef, OrderCancellation cancellation) {
        String reason = cancellation != null ? formatCancellationReason(cancellation.getReasonCode()) : "Chưa có lý do";
        String rejectionReason = cancellation != null ? cancellation.getRejectionReason() : null;
        return "Yêu cầu hủy cho đơn hàng " + orderRef + " đã bị từ chối."
                + " Lý do hủy: " + reason + "."
                + (StringUtils.hasText(rejectionReason) ? " Lý do từ chối: " + rejectionReason + "." : "");
    }

    private String buildRefundCompletedContent(String orderRef, PaymentClient.RefundResponse refund, String fallbackRefundId) {
        BigDecimal amount = refund != null ? refund.getAmount() : null;
        String providerReference = refund != null ? refund.getProviderReference() : null;
        Instant processedAt = refund != null ? refund.getProcessedAt() : null;
        return "Hoàn tiền cho đơn hàng " + orderRef + " đã hoàn tất."
                + (amount != null ? " Số tiền: " + amount.toPlainString() + "." : "")
                + (StringUtils.hasText(providerReference) ? " Mã giao dịch hoàn tiền: " + providerReference + "." : "")
                + " Mã hoàn tiền: " + (StringUtils.hasText(fallbackRefundId) ? fallbackRefundId : "không xác định") + "."
                + (processedAt != null ? " Thời gian xử lý: " + processedAt + "." : "");
    }

    private void sendEmailWithRetry(String userId, String email, String type, String title, String content, String htmlContent) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                notificationClient.sendEmail(NotificationClient.EmailRequest.builder()
                        .userId(userId)
                        .recipientEmail(email)
                        .type(type)
                        .title(title)
                        .content(content)
                        .htmlContent(htmlContent)
                        .build());
                return;
            } catch (Exception ex) {
                if (attempt == MAX_ATTEMPTS) {
                    log.warn("Failed to send {} notification for user {} after {} attempts: {}",
                            type, userId, attempt, ex.getMessage());
                }
            }
        }
    }

    private String resolveUserEmail(String userId) {
        try {
            var userResponse = userClient.getUserEmail(userId);
            return userResponse != null && userResponse.getData() != null
                    ? userResponse.getData().getEmail()
                    : null;
        } catch (Exception ex) {
            log.warn("Unable to resolve email for user {}: {}", userId, ex.getMessage());
            return null;
        }
    }

    private Optional<PaymentClient.RefundResponse> resolveRefund(String orderId) {
        try {
            var response = paymentClient.getRefundByOrderId(orderId);
            if (response != null && response.isSuccess() && response.getData() != null) {
                return Optional.of(response.getData());
            }
        } catch (Exception ex) {
            log.debug("Unable to resolve refund for order {}: {}", orderId, ex.getMessage());
        }
        return Optional.empty();
    }

    private String formatOrderReference(String orderId) {
        return StringUtils.hasText(orderId) && orderId.startsWith("#") ? orderId : "#" + orderId;
    }

    private String formatInstant(java.time.LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private String formatCancellationReason(String reasonCode) {
        if (!StringUtils.hasText(reasonCode)) {
            return "Chưa có thông tin";
        }
        return switch (reasonCode) {
            case "ORDERED_BY_MISTAKE" -> "Đặt nhầm đơn";
            case "CHANGE_PRODUCT_VARIANT" -> "Đổi mẫu / size / màu";
            case "CHANGE_DELIVERY_ADDRESS" -> "Đổi địa chỉ giao hàng";
            case "CHANGE_PAYMENT_METHOD" -> "Đổi phương thức thanh toán";
            case "DELIVERY_TOO_SLOW" -> "Giao hàng chậm";
            case "NO_LONGER_NEEDED" -> "Không còn nhu cầu";
            case "OTHER" -> "Khác";
            case "CUSTOMER_REQUESTED_OFFLINE" -> "Khách yêu cầu qua kênh khác";
            case "PRODUCT_UNAVAILABLE" -> "Hết hàng";
            case "INVALID_DELIVERY_INFORMATION" -> "Thông tin giao hàng không hợp lệ";
            case "FRAUD_SUSPECTED" -> "Nghi ngờ gian lận";
            case "DELIVERY_NOT_SUPPORTED" -> "Không hỗ trợ giao tới khu vực này";
            case "SYSTEM_ERROR" -> "Lỗi hệ thống";
            default -> reasonCode;
        };
    }
}
