package com.stylemind.order.service;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.order.entity.Order;
import com.stylemind.order.entity.OrderStatus;
import com.stylemind.order.exception.InvalidOrderStatusTransitionException;
import com.stylemind.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStatusServiceTest {

    @Mock OrderRepository orderRepository;

    @InjectMocks OrderStatusService orderStatusService;

    @Test
    void changeStatus_validTransition_savesAndReturnsUpdatedOrder() {
        Order order = order(OrderStatus.PENDING);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderStatusService.changeStatus(order, OrderStatus.CONFIRMED, "admin-1");

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(order);
    }

    @Test
    void changeStatus_invalidTransition_throws409_andNeverSaves() {
        Order order = order(OrderStatus.COMPLETED);

        assertThatThrownBy(() -> orderStatusService.changeStatus(order, OrderStatus.PENDING, "admin-1"))
                .isInstanceOf(InvalidOrderStatusTransitionException.class)
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus()).isEqualTo(409))
                .hasMessageContaining("COMPLETED")
                .hasMessageContaining("PENDING");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void changeStatus_fromTerminalState_alwaysRejected() {
        Order order = order(OrderStatus.CANCELLED);

        assertThatThrownBy(() -> orderStatusService.changeStatus(order, OrderStatus.PROCESSING, "admin-1"))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void changeStatus_byOrderId_notFound_throws404() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderStatusService.changeStatus("missing", OrderStatus.CONFIRMED, "admin-1"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus()).isEqualTo(404));
    }

    @Test
    void changeStatus_byOrderId_validTransition_looksUpAndSaves() {
        Order order = order(OrderStatus.PAID);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderStatusService.changeStatus("order-1", OrderStatus.PROCESSING, "admin-1");

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    private Order order(OrderStatus status) {
        return Order.builder()
                .id("order-1")
                .userId("user-1")
                .totalAmount(new BigDecimal("100000"))
                .orderStatus(status)
                .shippingAddress("123 Main Street")
                .build();
    }
}
