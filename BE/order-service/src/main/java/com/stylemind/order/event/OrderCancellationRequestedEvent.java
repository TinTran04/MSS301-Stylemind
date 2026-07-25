package com.stylemind.order.event;

public record OrderCancellationRequestedEvent(String orderId, String userId, String cancellationId) {
}
