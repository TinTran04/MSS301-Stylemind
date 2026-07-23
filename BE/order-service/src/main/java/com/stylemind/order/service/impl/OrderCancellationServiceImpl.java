package com.stylemind.order.service.impl;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import com.stylemind.order.dto.*;
import com.stylemind.order.entity.*;
import com.stylemind.order.event.*;
import com.stylemind.order.feign.PaymentClient;
import com.stylemind.order.repository.OrderCancellationRepository;
import com.stylemind.order.repository.OrderRepository;
import com.stylemind.order.service.OrderCancellationService;
import com.stylemind.order.service.OrderStatusService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderCancellationServiceImpl implements OrderCancellationService {

    private static final EnumSet<OrderStatus> CUSTOMER_REQUEST_STATUSES =
            EnumSet.of(OrderStatus.PAID, OrderStatus.CONFIRMED, OrderStatus.PROCESSING);
    private static final EnumSet<OrderStatus> ADMIN_CANCELLABLE_STATUSES =
            EnumSet.of(OrderStatus.PENDING, OrderStatus.PAYMENT_PENDING, OrderStatus.PAID, OrderStatus.CONFIRMED, OrderStatus.PROCESSING);

    private final OrderRepository orderRepository;
    private final OrderCancellationRepository cancellationRepository;
    private final PaymentClient paymentClient;
    private final OrderStatusService orderStatusService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public OrderCancellationResponse requestCustomerCancellation(
            String userId,
            String orderId,
            String idempotencyKey,
            CreateOrderCancellationRequest request) {
        String normalizedKey = trim(idempotencyKey);
        if (StringUtils.hasText(normalizedKey)) {
            var existing = cancellationRepository.findByRequestedByAndOrderIdAndIdempotencyKey(userId, orderId, normalizedKey);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", 404));
        String reason = validateCustomerReason(request);

        if (cancellationRepository.existsByOrderIdAndStatus(orderId, OrderCancellationStatus.REQUESTED)) {
            throw new BusinessException("ORDER_CANCELLATION_ALREADY_PENDING", "Đơn hàng đã có yêu cầu hủy đang chờ duyệt.", 409);
        }

        OrderStatus current = order.getOrderStatus();
        if (current == OrderStatus.PENDING) {
            return toResponse(approveDirectCancellation(order, userId, OrderCancellationType.CUSTOMER_DIRECT, reason,
                    trim(request.getCustomerNote()), null, normalizedKey));
        }

        if (current == OrderStatus.PAYMENT_PENDING) {
            OrderCancellation draft = saveCancellation(newCancellation(order, userId, OrderCancellationType.CUSTOMER_DIRECT,
                    OrderCancellationStatus.REQUESTED, reason, trim(request.getCustomerNote()), null, normalizedKey));
            PaymentClient.PaymentCancellationResponse payment = cancelPayment(orderId, draft.getId());
            if (payment.isPaymentReceived()) {
                orderStatusService.changeStatus(order, OrderStatus.PAID, "PAYMENT_SYNC");
                draft.setCancellationType(OrderCancellationType.CUSTOMER_REQUEST);
                draft.setStatus(OrderCancellationStatus.REQUESTED);
                draft = saveCancellation(draft);
                eventPublisher.publishEvent(new OrderCancellationRequestedEvent(orderId, userId, draft.getId()));
                return toResponse(draft);
            }
            draft.setStatus(OrderCancellationStatus.APPROVED);
            draft.setReviewedBy(userId);
            draft.setReviewedAt(LocalDateTime.now());
            draft.setApprovedAt(LocalDateTime.now());
            draft = saveCancellation(draft);
            Order cancelled = orderStatusService.changeStatus(order, OrderStatus.CANCELLED, userId);
            eventPublisher.publishEvent(new OrderCancellationApprovedEvent(cancelled.getId(), cancelled.getUserId(), draft.getId()));
            return toResponse(draft);
        }

        if (CUSTOMER_REQUEST_STATUSES.contains(current)) {
            OrderCancellation cancellation = saveRequestedCancellation(order, userId, OrderCancellationType.CUSTOMER_REQUEST,
                    reason, trim(request.getCustomerNote()), null, normalizedKey);
            eventPublisher.publishEvent(new OrderCancellationRequestedEvent(orderId, userId, cancellation.getId()));
            return toResponse(cancellation);
        }

        throw new BusinessException("ORDER_CANCELLATION_NOT_ALLOWED", "Không thể yêu cầu hủy đơn hàng ở trạng thái hiện tại.", 409);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderCancellationResponse> getCustomerCancellations(String userId, String orderId) {
        if (!orderRepository.findByIdAndUserId(orderId, userId).isPresent()) {
            throw new BusinessException("ORDER_NOT_FOUND", "Order not found", 404);
        }
        return cancellationRepository.findByUserIdAndOrderIdOrderByCreatedAtDesc(userId, orderId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public OrderCancellationResponse adminCancelOrder(String adminUserId, String orderId, AdminCancelOrderRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", 404));
        if (!ADMIN_CANCELLABLE_STATUSES.contains(order.getOrderStatus())) {
            throw new BusinessException("ORDER_CANCELLATION_NOT_ALLOWED", "Không thể hủy đơn hàng ở trạng thái hiện tại.", 409);
        }
        if (cancellationRepository.existsByOrderIdAndStatus(orderId, OrderCancellationStatus.REQUESTED)) {
            throw new BusinessException("ORDER_CANCELLATION_ALREADY_PENDING", "Đơn hàng đang có yêu cầu hủy chờ duyệt.", 409);
        }
        String reason = validateAdminReason(request);
        OrderCancellation cancellation = newCancellation(order, adminUserId, OrderCancellationType.ADMIN_DIRECT,
                OrderCancellationStatus.APPROVED, reason, null, trim(request.getAdminNote()), null);
        if (order.getOrderStatus() == OrderStatus.PAYMENT_PENDING) {
            PaymentClient.PaymentCancellationResponse payment = cancelPayment(orderId, cancellation.getId());
            if (payment.isPaymentReceived()) {
                order = orderStatusService.changeStatus(order, OrderStatus.PAID, "PAYMENT_SYNC");
                createRefund(orderId, cancellation.getId());
            }
        } else if (isPaidLike(order)) {
            createRefund(orderId, cancellation.getId());
        }
        return toResponse(approveAndSave(cancellation, order, adminUserId, true));
    }

    @Override
    public OrderCancellationResponse approveCancellation(String adminUserId, String cancellationId) {
        OrderCancellation cancellation = cancellationRepository.findByIdForUpdate(cancellationId)
                .orElseThrow(() -> new BusinessException("ORDER_CANCELLATION_NOT_FOUND", "Cancellation request not found", 404));
        if (cancellation.getStatus() != OrderCancellationStatus.REQUESTED) {
            throw new BusinessException("ORDER_CANCELLATION_ALREADY_RESOLVED", "Yêu cầu hủy đã được xử lý.", 409);
        }
        Order order = orderRepository.findById(cancellation.getOrderId())
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", 404));
        if (!CUSTOMER_REQUEST_STATUSES.contains(order.getOrderStatus())) {
            throw new BusinessException("ORDER_CANCELLATION_NOT_ALLOWED", "Trạng thái đơn hàng đã thay đổi, không thể duyệt hủy.", 409);
        }
        createRefund(order.getId(), cancellation.getId());
        Order cancelled = orderStatusService.changeStatus(order, OrderStatus.CANCELLED, adminUserId);
        cancellation.setStatus(OrderCancellationStatus.APPROVED);
        cancellation.setReviewedBy(adminUserId);
        cancellation.setReviewedAt(LocalDateTime.now());
        cancellation.setApprovedAt(LocalDateTime.now());
        OrderCancellation saved = cancellationRepository.save(cancellation);
        eventPublisher.publishEvent(new OrderCancellationApprovedEvent(cancelled.getId(), cancelled.getUserId(), saved.getId()));
        return toResponse(saved);
    }

    @Override
    public OrderCancellationResponse rejectCancellation(String adminUserId, String cancellationId, RejectOrderCancellationRequest request) {
        OrderCancellation cancellation = cancellationRepository.findByIdForUpdate(cancellationId)
                .orElseThrow(() -> new BusinessException("ORDER_CANCELLATION_NOT_FOUND", "Cancellation request not found", 404));
        if (cancellation.getStatus() != OrderCancellationStatus.REQUESTED) {
            throw new BusinessException("ORDER_CANCELLATION_ALREADY_RESOLVED", "Yêu cầu hủy đã được xử lý.", 409);
        }
        if (!StringUtils.hasText(request.getRejectionReason())) {
            throw new BusinessException("ORDER_CANCELLATION_REJECTION_REASON_REQUIRED", "Vui lòng nhập lý do từ chối.", 400);
        }
        cancellation.setStatus(OrderCancellationStatus.REJECTED);
        cancellation.setReviewedBy(adminUserId);
        cancellation.setReviewedAt(LocalDateTime.now());
        cancellation.setRejectionReason(trim(request.getRejectionReason()));
        OrderCancellation saved = cancellationRepository.save(cancellation);
        eventPublisher.publishEvent(new OrderCancellationRejectedEvent(saved.getOrderId(), saved.getUserId(), saved.getId()));
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderCancellationSummaryResponse getPendingSummary() {
        return OrderCancellationSummaryResponse.builder()
                .pendingCount(cancellationRepository.countByStatus(OrderCancellationStatus.REQUESTED))
                .build();
    }

    @Override
    public RefundSummaryResponse completeRefund(String adminUserId, String orderId, String refundId, CompleteOrderRefundRequest request) {
        ensureOrderExists(orderId);
        PaymentClient.RefundResponse refund = unwrapRefund(paymentClient.completeRefund(refundId,
                PaymentClient.CompleteRefundRequest.builder()
                        .providerReference(request.getProviderReference())
                        .proofUrl(request.getProofUrl())
                        .note(request.getNote())
                        .processedBy(adminUserId)
                        .build()));
        ensureRefundBelongsToOrder(orderId, refund);
        eventPublisher.publishEvent(new RefundCompletedEvent(orderId, resolveOrderUserId(orderId), refundId));
        return mapRefund(refund);
    }

    @Override
    public RefundSummaryResponse failRefund(String adminUserId, String orderId, String refundId, FailOrderRefundRequest request) {
        ensureOrderExists(orderId);
        PaymentClient.RefundResponse refund = unwrapRefund(paymentClient.failRefund(refundId,
                PaymentClient.FailRefundRequest.builder()
                        .failureReason(request.getFailureReason())
                        .processedBy(adminUserId)
                        .build()));
        ensureRefundBelongsToOrder(orderId, refund);
        return mapRefund(refund);
    }

    private OrderCancellation approveDirectCancellation(
            Order order,
            String actorId,
            OrderCancellationType type,
            String reason,
            String customerNote,
            String adminNote,
            String idempotencyKey) {
        OrderCancellation cancellation = newCancellation(order, actorId, type, OrderCancellationStatus.APPROVED,
                reason, customerNote, adminNote, idempotencyKey);
        return approveAndSave(cancellation, order, actorId, true);
    }

    private OrderCancellation approveAndSave(OrderCancellation cancellation, Order order, String actorId, boolean publishApproved) {
        OrderCancellation saved = saveCancellation(cancellation);
        Order cancelled = orderStatusService.changeStatus(order, OrderStatus.CANCELLED, actorId);
        saved.setApprovedAt(LocalDateTime.now());
        saved.setReviewedAt(LocalDateTime.now());
        saved.setReviewedBy(actorId);
        saved = cancellationRepository.save(saved);
        if (publishApproved) {
            eventPublisher.publishEvent(new OrderCancellationApprovedEvent(cancelled.getId(), cancelled.getUserId(), saved.getId()));
        }
        return saved;
    }

    private OrderCancellation saveRequestedCancellation(
            Order order,
            String actorId,
            OrderCancellationType type,
            String reason,
            String customerNote,
            String adminNote,
            String idempotencyKey) {
        return saveCancellation(newCancellation(order, actorId, type, OrderCancellationStatus.REQUESTED,
                reason, customerNote, adminNote, idempotencyKey));
    }

    private OrderCancellation newCancellation(
            Order order,
            String actorId,
            OrderCancellationType type,
            OrderCancellationStatus status,
            String reason,
            String customerNote,
            String adminNote,
            String idempotencyKey) {
        LocalDateTime now = LocalDateTime.now();
        return OrderCancellation.builder()
                .id(StringUtil.generateUniqueId())
                .orderId(order.getId())
                .userId(order.getUserId())
                .cancellationType(type)
                .status(status)
                .reasonCode(reason)
                .customerNote(customerNote)
                .adminNote(adminNote)
                .requestedBy(actorId)
                .reviewedBy(status == OrderCancellationStatus.APPROVED ? actorId : null)
                .requestedAt(now)
                .reviewedAt(status == OrderCancellationStatus.APPROVED ? now : null)
                .approvedAt(status == OrderCancellationStatus.APPROVED ? now : null)
                .idempotencyKey(idempotencyKey)
                .build();
    }

    private OrderCancellation saveCancellation(OrderCancellation cancellation) {
        try {
            return cancellationRepository.save(cancellation);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("ORDER_CANCELLATION_ALREADY_PENDING", "Đơn hàng đã có yêu cầu hủy đang chờ duyệt.", 409);
        }
    }

    private PaymentClient.PaymentCancellationResponse cancelPayment(String orderId, String cancellationId) {
        try {
            var response = paymentClient.cancelPayment(orderId,
                    PaymentClient.CancelPaymentRequest.builder().orderCancellationId(cancellationId).build());
            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new BusinessException("ORDER_CANCELLATION_PAYMENT_SYNC_FAILED", "Không thể đồng bộ hủy thanh toán.", 502);
            }
            return response.getData();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Payment cancellation sync failed for order {}: {}", orderId, ex.getMessage());
            throw new BusinessException("ORDER_CANCELLATION_PAYMENT_SYNC_FAILED", "Không thể đồng bộ hủy thanh toán.", 502);
        }
    }

    private RefundSummaryResponse createRefund(String orderId, String cancellationId) {
        try {
            PaymentClient.RefundResponse refund = unwrapRefund(paymentClient.createRefund(
                    PaymentClient.CreateRefundRequest.builder()
                            .orderId(orderId)
                            .orderCancellationId(cancellationId)
                            .build()));
            return mapRefund(refund);
        } catch (FeignException ex) {
            if (ex.status() == 409) {
                return null;
            }
            throw new BusinessException("ORDER_CANCELLATION_REFUND_SYNC_FAILED", "Không thể tạo yêu cầu hoàn tiền.", 502);
        } catch (Exception ex) {
            log.warn("Refund sync failed for order {}: {}", orderId, ex.getMessage());
            throw new BusinessException("ORDER_CANCELLATION_REFUND_SYNC_FAILED", "Không thể tạo yêu cầu hoàn tiền.", 502);
        }
    }

    private boolean isPaidLike(Order order) {
        return order.getOrderStatus() == OrderStatus.PAID
                || order.getOrderStatus() == OrderStatus.CONFIRMED
                || order.getOrderStatus() == OrderStatus.PROCESSING;
    }

    private String validateCustomerReason(CreateOrderCancellationRequest request) {
        String reason = normalizeReason(request == null ? null : request.getReasonCode());
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException("ORDER_CANCELLATION_REASON_REQUIRED", "Vui lòng chọn lý do hủy đơn.", 400);
        }
        try {
            CustomerCancellationReason parsed = CustomerCancellationReason.valueOf(reason);
            if (parsed == CustomerCancellationReason.OTHER && (request == null || !StringUtils.hasText(request.getCustomerNote()))) {
                throw new BusinessException("ORDER_CANCELLATION_REASON_REQUIRED", "Vui lòng nhập ghi chú khi chọn lý do khác.", 400);
            }
            return parsed.name();
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("ORDER_CANCELLATION_REASON_REQUIRED", "Lý do hủy đơn không hợp lệ.", 400);
        }
    }

    private String validateAdminReason(AdminCancelOrderRequest request) {
        String reason = normalizeReason(request == null ? null : request.getReasonCode());
        if (!StringUtils.hasText(reason) || request == null || !StringUtils.hasText(request.getAdminNote())) {
            throw new BusinessException("ORDER_CANCELLATION_REASON_REQUIRED", "Vui lòng nhập lý do và ghi chú hủy đơn.", 400);
        }
        try {
            return AdminCancellationReason.valueOf(reason).name();
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("ORDER_CANCELLATION_REASON_REQUIRED", "Lý do hủy đơn không hợp lệ.", 400);
        }
    }

    private String normalizeReason(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void ensureOrderExists(String orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new BusinessException("ORDER_NOT_FOUND", "Order not found", 404);
        }
    }

    private String resolveOrderUserId(String orderId) {
        return orderRepository.findById(orderId)
                .map(Order::getUserId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", 404));
    }

    private PaymentClient.RefundResponse unwrapRefund(com.stylemind.common.dto.ApiResponse<PaymentClient.RefundResponse> response) {
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new BusinessException("ORDER_CANCELLATION_REFUND_SYNC_FAILED", "Không thể đồng bộ hoàn tiền.", 502);
        }
        return response.getData();
    }

    private void ensureRefundBelongsToOrder(String orderId, PaymentClient.RefundResponse refund) {
        if (refund == null || !orderId.equals(refund.getOrderId())) {
            throw new BusinessException("ORDER_CANCELLATION_REFUND_SYNC_FAILED", "Thông tin hoàn tiền không khớp đơn hàng.", 502);
        }
    }

    private RefundSummaryResponse mapRefund(PaymentClient.RefundResponse refund) {
        if (refund == null) {
            return null;
        }
        return RefundSummaryResponse.builder()
                .id(refund.getId())
                .orderId(refund.getOrderId())
                .paymentTransactionId(refund.getPaymentTransactionId())
                .orderCancellationId(refund.getOrderCancellationId())
                .amount(refund.getAmount())
                .status(refund.getStatus())
                .method(refund.getMethod())
                .providerReference(refund.getProviderReference())
                .proofUrl(refund.getProofUrl())
                .note(refund.getNote())
                .processedBy(refund.getProcessedBy())
                .processedAt(refund.getProcessedAt())
                .failureReason(refund.getFailureReason())
                .createdAt(refund.getCreatedAt())
                .updatedAt(refund.getUpdatedAt())
                .build();
    }

    private OrderCancellationResponse toResponse(OrderCancellation cancellation) {
        return OrderCancellationResponse.builder()
                .id(cancellation.getId())
                .orderId(cancellation.getOrderId())
                .userId(cancellation.getUserId())
                .cancellationType(cancellation.getCancellationType().name())
                .status(cancellation.getStatus().name())
                .reasonCode(cancellation.getReasonCode())
                .customerNote(cancellation.getCustomerNote())
                .adminNote(cancellation.getAdminNote())
                .rejectionReason(cancellation.getRejectionReason())
                .requestedBy(cancellation.getRequestedBy())
                .reviewedBy(cancellation.getReviewedBy())
                .requestedAt(toInstant(cancellation.getRequestedAt()))
                .reviewedAt(toInstant(cancellation.getReviewedAt()))
                .approvedAt(toInstant(cancellation.getApprovedAt()))
                .createdAt(toInstant(cancellation.getCreatedAt()))
                .updatedAt(toInstant(cancellation.getUpdatedAt()))
                .build();
    }

    private java.time.Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(java.time.ZoneId.systemDefault()).toInstant();
    }
}
