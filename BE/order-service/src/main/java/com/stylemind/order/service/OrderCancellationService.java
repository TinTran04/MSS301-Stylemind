package com.stylemind.order.service;

import com.stylemind.order.dto.*;

import java.util.List;

public interface OrderCancellationService {
    OrderCancellationResponse requestCustomerCancellation(String userId, String orderId, String idempotencyKey, CreateOrderCancellationRequest request);
    List<OrderCancellationResponse> getCustomerCancellations(String userId, String orderId);
    OrderCancellationResponse adminCancelOrder(String adminUserId, String orderId, AdminCancelOrderRequest request);
    OrderCancellationResponse approveCancellation(String adminUserId, String cancellationId);
    OrderCancellationResponse rejectCancellation(String adminUserId, String cancellationId, RejectOrderCancellationRequest request);
    OrderCancellationSummaryResponse getPendingSummary();
    RefundSummaryResponse completeRefund(String adminUserId, String orderId, String refundId, CompleteOrderRefundRequest request);
    RefundSummaryResponse failRefund(String adminUserId, String orderId, String refundId, FailOrderRefundRequest request);
}
