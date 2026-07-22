package com.stylemind.order.service.impl;

import com.stylemind.order.entity.OrderStatus;
import com.stylemind.order.event.OrderStatusChangedEvent;
import com.stylemind.order.feign.NotificationClient;
import com.stylemind.order.feign.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusNotificationListener {

    private static final int MAX_ATTEMPTS = 3;

    private final UserClient userClient;
    private final NotificationClient notificationClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        NotificationCopy copy = copyFor(event.newStatus(), event.orderId());
        if (copy == null) {
            return;
        }

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String email = resolveUserEmail(event.userId());
                if (!StringUtils.hasText(email)) {
                    log.warn("No email on file for user {} - skipping {} notification for order {}",
                            event.userId(), copy.type(), event.orderId());
                    return;
                }

                notificationClient.sendEmail(NotificationClient.EmailRequest.builder()
                        .userId(event.userId())
                        .recipientEmail(email)
                        .type(copy.type())
                        .title(copy.title())
                        .content(copy.content())
                        .build());
                return;
            } catch (Exception ex) {
                if (attempt == MAX_ATTEMPTS) {
                    log.warn("Failed to send {} notification for order {} after {} attempts: {}",
                            copy.type(), event.orderId(), attempt, ex.getMessage());
                } else {
                    log.debug("Notification attempt {} failed for order {}, retrying: {}",
                            attempt, event.orderId(), ex.getMessage());
                }
            }
        }
    }

    private String resolveUserEmail(String userId) {
        var userResponse = userClient.getUserEmail(userId);
        return userResponse != null && userResponse.getData() != null
                ? userResponse.getData().getEmail()
                : null;
    }

    private NotificationCopy copyFor(OrderStatus status, String orderId) {
        String ref = formatOrderReference(orderId);
        return switch (status) {
            case PAYMENT_PENDING -> new NotificationCopy(
                    "ORDER_PAYMENT_PENDING",
                    "Đơn hàng chờ thanh toán",
                    "Đơn hàng " + ref + " đã được tạo và đang chờ thanh toán.");
            case PAID -> new NotificationCopy(
                    "ORDER_PAID",
                    "Thanh toán thành công",
                    "Thanh toán cho đơn hàng " + ref + " đã được ghi nhận thành công.");
            case CONFIRMED -> new NotificationCopy(
                    "ORDER_CONFIRMED",
                    "Đơn hàng đã được xác nhận",
                    "Đơn hàng " + ref + " của bạn đã được xác nhận. StyleMind đang chuẩn bị xử lý đơn.");
            case PROCESSING -> new NotificationCopy(
                    "ORDER_PROCESSING",
                    "Đơn hàng đang được xử lý",
                    "Đơn hàng " + ref + " đang được StyleMind chuẩn bị và đóng gói.");
            case SHIPPED -> new NotificationCopy(
                    "ORDER_SHIPPED",
                    "Đơn hàng đang giao",
                    "Đơn hàng " + ref + " đã rời kho và đang trên đường giao đến bạn.");
            case COMPLETED -> new NotificationCopy(
                    "ORDER_COMPLETED",
                    "Đơn hàng đã giao thành công",
                    "Đơn hàng " + ref + " đã được giao thành công. Cảm ơn bạn đã mua sắm tại StyleMind.");
            case CANCELLED -> new NotificationCopy(
                    "ORDER_CANCELLED",
                    "Đơn hàng đã bị hủy",
                    "Đơn hàng " + ref + " đã bị hủy. Nếu cần hỗ trợ, vui lòng liên hệ StyleMind.");
            case EXPIRED -> new NotificationCopy(
                    "ORDER_EXPIRED",
                    "Thanh toán đã hết hạn",
                    "Đơn hàng " + ref + " đã hết thời gian thanh toán và không còn hiệu lực.");
            case FAILED -> new NotificationCopy(
                    "ORDER_FAILED",
                    "Đơn hàng không thành công",
                    "Đơn hàng " + ref + " không thể hoàn tất. Vui lòng thử lại hoặc chọn phương thức thanh toán khác.");
            case PENDING -> null;
        };
    }

    private String formatOrderReference(String orderId) {
        return StringUtils.hasText(orderId) && orderId.startsWith("#") ? orderId : "#" + orderId;
    }

    private record NotificationCopy(String type, String title, String content) {
    }
}
