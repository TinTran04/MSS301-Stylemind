package com.stylemind.order.entity;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {
    PENDING,
    PAYMENT_PENDING,
    PAID,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    COMPLETED,
    CANCELLED,
    EXPIRED,
    FAILED;

    public Set<OrderStatus> allowedTransitions() {
        return switch (this) {
            case PENDING -> EnumSet.of(PAYMENT_PENDING, CONFIRMED, CANCELLED);
            case PAYMENT_PENDING -> EnumSet.of(PAID, EXPIRED, FAILED, CANCELLED);
            case PAID -> EnumSet.of(CONFIRMED, PROCESSING, CANCELLED);
            case CONFIRMED -> EnumSet.of(PROCESSING, CANCELLED);
            case PROCESSING -> EnumSet.of(SHIPPED, CANCELLED);
            case SHIPPED -> EnumSet.of(COMPLETED);
            case COMPLETED, CANCELLED, EXPIRED, FAILED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canTransitionTo(OrderStatus target) {
        return allowedTransitions().contains(target);
    }

    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }
}
