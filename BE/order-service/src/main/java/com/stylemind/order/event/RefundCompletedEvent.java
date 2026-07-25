package com.stylemind.order.event;

public record RefundCompletedEvent(String orderId, String userId, String refundId) {
}
