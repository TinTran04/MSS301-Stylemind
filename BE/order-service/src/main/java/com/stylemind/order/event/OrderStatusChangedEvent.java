package com.stylemind.order.event;

import com.stylemind.order.entity.OrderStatus;

public record OrderStatusChangedEvent(
        String orderId,
        String userId,
        OrderStatus previousStatus,
        OrderStatus newStatus
) {
}
