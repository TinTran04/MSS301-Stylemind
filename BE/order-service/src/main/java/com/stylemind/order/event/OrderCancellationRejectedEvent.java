package com.stylemind.order.event;

public record OrderCancellationRejectedEvent(String orderId, String userId, String cancellationId) {
}
