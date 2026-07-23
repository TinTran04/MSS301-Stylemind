package com.stylemind.order.event;

public record OrderCancellationApprovedEvent(String orderId, String userId, String cancellationId) {
}
