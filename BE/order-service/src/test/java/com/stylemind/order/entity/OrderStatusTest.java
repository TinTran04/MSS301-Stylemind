package com.stylemind.order.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void pending_allowsPaymentPendingConfirmedAndCancelled() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.PAYMENT_PENDING)).isTrue();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.PROCESSING)).isFalse();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.COMPLETED)).isFalse();
    }

    @Test
    void paymentPending_allowsPaidExpiredFailedAndCancelled() {
        assertThat(OrderStatus.PAYMENT_PENDING.canTransitionTo(OrderStatus.PAID)).isTrue();
        assertThat(OrderStatus.PAYMENT_PENDING.canTransitionTo(OrderStatus.EXPIRED)).isTrue();
        assertThat(OrderStatus.PAYMENT_PENDING.canTransitionTo(OrderStatus.FAILED)).isTrue();
        assertThat(OrderStatus.PAYMENT_PENDING.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.PAYMENT_PENDING.canTransitionTo(OrderStatus.PROCESSING)).isFalse();
    }

    @Test
    void shipped_onlyAllowsCompleted() {
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.COMPLETED)).isTrue();
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.PROCESSING)).isFalse();
    }

    @Test
    void terminalStates_rejectEveryTransition() {
        for (OrderStatus terminal : new OrderStatus[]{OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.EXPIRED, OrderStatus.FAILED}) {
            assertThat(terminal.isTerminal()).isTrue();
            for (OrderStatus target : OrderStatus.values()) {
                assertThat(terminal.canTransitionTo(target)).as("%s -> %s", terminal, target).isFalse();
            }
        }
    }

    @Test
    void completedToPending_isRejected() {
        assertThat(OrderStatus.COMPLETED.canTransitionTo(OrderStatus.PENDING)).isFalse();
    }
}
