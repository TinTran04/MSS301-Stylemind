package com.stylemind.order.job;

import com.stylemind.order.entity.Order;
import com.stylemind.order.entity.OrderStatus;
import com.stylemind.order.feign.PaymentClient;
import com.stylemind.order.repository.OrderRepository;
import com.stylemind.order.service.OrderStatusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// F7: SePay expiry/compensation - a SePay order left in PAYMENT_PENDING past the
// timeout window must be compensated to EXPIRED without any customer action,
// and a single failure must not stop the rest of the batch from being processed.
@ExtendWith(MockitoExtension.class)
class OrderTimeoutJobTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderStatusService orderStatusService;
    @Mock PaymentClient paymentClient;

    @InjectMocks OrderTimeoutJob orderTimeoutJob;

    @Test
    void expireStalePaymentPendingOrders_noStaleOrders_doesNothing() {
        ReflectionTestUtils.setField(orderTimeoutJob, "paymentTimeoutMinutes", 30L);
        when(orderRepository.findByOrderStatusAndCreatedAtBefore(eq(OrderStatus.PAYMENT_PENDING), any()))
                .thenReturn(List.of());

        orderTimeoutJob.expireStalePaymentPendingOrders();

        verify(orderStatusService, never()).changeStatus(any(Order.class), any(), anyString());
    }

    @Test
    void expireStalePaymentPendingOrders_staleOrder_isTransitionedToExpiredBySystemActor() {
        ReflectionTestUtils.setField(orderTimeoutJob, "paymentTimeoutMinutes", 30L);
        Order stale = order("order-1", OrderStatus.PAYMENT_PENDING);
        when(orderRepository.findByOrderStatusAndCreatedAtBefore(eq(OrderStatus.PAYMENT_PENDING), any()))
                .thenReturn(List.of(stale));

        orderTimeoutJob.expireStalePaymentPendingOrders();

        ArgumentCaptor<String> actorCaptor = ArgumentCaptor.forClass(String.class);
        verify(orderStatusService).changeStatus(eq(stale), eq(OrderStatus.EXPIRED), actorCaptor.capture());
        verify(paymentClient).expirePaymentByOrderId("order-1");
        assertThatActorLooksLikeSystem(actorCaptor.getValue());
    }

    @Test
    void expireStalePaymentPendingOrders_queriesWithConfiguredTimeoutWindow() {
        ReflectionTestUtils.setField(orderTimeoutJob, "paymentTimeoutMinutes", 45L);
        when(orderRepository.findByOrderStatusAndCreatedAtBefore(eq(OrderStatus.PAYMENT_PENDING), any()))
                .thenReturn(List.of());

        LocalDateTime before = LocalDateTime.now().minusMinutes(45);
        orderTimeoutJob.expireStalePaymentPendingOrders();
        LocalDateTime after = LocalDateTime.now().minusMinutes(45);

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderRepository).findByOrderStatusAndCreatedAtBefore(eq(OrderStatus.PAYMENT_PENDING), cutoffCaptor.capture());
        LocalDateTime cutoff = cutoffCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(cutoff).isBetween(before.minusSeconds(2), after.plusSeconds(2));
    }

    @Test
    void expireStalePaymentPendingOrders_oneOrderFailsToTransition_othersStillProcessed() {
        ReflectionTestUtils.setField(orderTimeoutJob, "paymentTimeoutMinutes", 30L);
        Order failing = order("order-fail", OrderStatus.PAYMENT_PENDING);
        Order ok = order("order-ok", OrderStatus.PAYMENT_PENDING);
        when(orderRepository.findByOrderStatusAndCreatedAtBefore(eq(OrderStatus.PAYMENT_PENDING), any()))
                .thenReturn(List.of(failing, ok));
        when(orderStatusService.changeStatus(eq(failing), eq(OrderStatus.EXPIRED), anyString()))
                .thenThrow(new RuntimeException("db hiccup"));

        orderTimeoutJob.expireStalePaymentPendingOrders();

        verify(orderStatusService).changeStatus(eq(ok), eq(OrderStatus.EXPIRED), anyString());
    }

    private void assertThatActorLooksLikeSystem(String actor) {
        org.assertj.core.api.Assertions.assertThat(actor).containsIgnoringCase("SYSTEM");
    }

    private Order order(String id, OrderStatus status) {
        return Order.builder()
                .id(id)
                .userId("user-1")
                .totalAmount(new BigDecimal("100000"))
                .orderStatus(status)
                .shippingAddress("123 Main Street")
                .build();
    }
}
