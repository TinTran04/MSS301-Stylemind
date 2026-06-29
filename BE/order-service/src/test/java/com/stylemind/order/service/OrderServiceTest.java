package com.stylemind.order.service;

import com.stylemind.cart.dto.CartItemResponse;
import com.stylemind.cart.dto.CartResponse;
import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.order.dto.CreateOrderRequest;
import com.stylemind.order.dto.OrderResponse;
import com.stylemind.order.entity.Order;
import com.stylemind.order.entity.OrderItem;
import com.stylemind.order.feign.CartClient;
import com.stylemind.order.feign.PaymentClient;
import com.stylemind.order.feign.ProductClient;
import com.stylemind.order.repository.OrderItemRepository;
import com.stylemind.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock CartClient cartClient;
    @Mock PaymentClient paymentClient;
    @Mock ProductClient productClient;

    @InjectMocks OrderService orderService;

    // ─── createOrder ─────────────────────────────────────────────────────────

    @Test
    void createOrder_emptyCart_throws() {
        CartResponse emptyCart = new CartResponse();
        emptyCart.setItems(List.of());
        when(cartClient.getCart(any(), any())).thenReturn(ApiResponse.success("ok", emptyCart));

        assertThatThrownBy(() -> orderService.createOrder("user-1", "Bearer tok", codReq()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Giỏ hàng trống");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_cod_fulfilledWithoutPaymentCall() {
        CartResponse cart = cartWithItems();
        when(cartClient.getCart(any(), any())).thenReturn(ApiResponse.success("ok", cart));
        when(orderRepository.save(any())).thenAnswer(inv -> savedOrder(inv.getArgument(0)));
        when(orderItemRepository.save(any())).thenAnswer(inv -> savedItem(inv.getArgument(0)));
        when(cartClient.clearCart(any())).thenReturn(ApiResponse.success("ok", null));

        OrderResponse result = orderService.createOrder("user-1", "Bearer tok", codReq());

        assertThat(result.getOrderStatus()).isEqualTo("FULFILLED");
        verify(paymentClient, never()).processPayment(any());
        verify(cartClient).clearCart("Bearer tok");
    }

    @Test
    void createOrder_onlinePayment_callsPaymentAndClearsCart() {
        CartResponse cart = cartWithItems();
        when(cartClient.getCart(any(), any())).thenReturn(ApiResponse.success("ok", cart));
        when(orderRepository.save(any())).thenAnswer(inv -> savedOrder(inv.getArgument(0)));
        when(orderItemRepository.save(any())).thenAnswer(inv -> savedItem(inv.getArgument(0)));
        when(paymentClient.processPayment(any()))
                .thenReturn(ApiResponse.success("ok", paymentResponse("COMPLETED")));
        when(cartClient.clearCart(any())).thenReturn(ApiResponse.success("ok", null));

        OrderResponse result = orderService.createOrder("user-1", "Bearer tok", onlineReq("txn-1"));

        assertThat(result.getOrderStatus()).isEqualTo("FULFILLED");
        verify(paymentClient).processPayment(argThat(r -> "txn-1".equals(r.getTransactionId())));
        verify(cartClient).clearCart("Bearer tok");
    }

    @Test
    void createOrder_paymentFails_cancelledAndThrows() {
        CartResponse cart = cartWithItems();
        when(cartClient.getCart(any(), any())).thenReturn(ApiResponse.success("ok", cart));
        when(orderRepository.save(any())).thenAnswer(inv -> savedOrder(inv.getArgument(0)));
        when(orderItemRepository.save(any())).thenAnswer(inv -> savedItem(inv.getArgument(0)));
        when(paymentClient.processPayment(any())).thenThrow(new RuntimeException("gateway down"));

        assertThatThrownBy(() -> orderService.createOrder("user-1", "Bearer tok", onlineReq("txn-fail")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Thanh toán thất bại");

        // Order saved twice: PENDING (initial) + CANCELLED (on payment failure)
        verify(orderRepository, times(2)).save(any());
        // Cart must NOT be cleared on payment failure
        verify(cartClient, never()).clearCart(any());
    }

    @Test
    void createOrder_onlineMissingTransactionId_throws() {
        CartResponse cart = cartWithItems();
        when(cartClient.getCart(any(), any())).thenReturn(ApiResponse.success("ok", cart));
        when(orderRepository.save(any())).thenAnswer(inv -> savedOrder(inv.getArgument(0)));
        when(orderItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateOrderRequest req = new CreateOrderRequest();
        req.setShippingAddress("123 Street");
        req.setPaymentMethod("online_simulated");
        // transactionId intentionally null

        assertThatThrownBy(() -> orderService.createOrder("user-1", "Bearer tok", req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("transactionId");
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private CartResponse cartWithItems() {
        CartItemResponse item = new CartItemResponse();
        item.setVariantId("var-A");
        item.setQuantity(1);
        item.setIsAiRecommended(false);

        CartResponse cart = new CartResponse();
        cart.setItems(List.of(item));
        cart.setTotalAmount(new BigDecimal("150000"));
        return cart;
    }

    private CreateOrderRequest codReq() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setShippingAddress("123 Main Street");
        req.setPaymentMethod("cod");
        return req;
    }

    private CreateOrderRequest onlineReq(String txnId) {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setShippingAddress("123 Main Street");
        req.setPaymentMethod("online_simulated");
        req.setTransactionId(txnId);
        return req;
    }

    private Order savedOrder(Order o) {
        if (o.getId() == null) o.setId("order-generated-id");
        if (o.getCreatedAt() == null) o.setCreatedAt(LocalDateTime.now());
        if (o.getUpdatedAt() == null) o.setUpdatedAt(LocalDateTime.now());
        return o;
    }

    private OrderItem savedItem(OrderItem item) {
        if (item.getCreatedAt() == null) item.setCreatedAt(LocalDateTime.now());
        if (item.getUpdatedAt() == null) item.setUpdatedAt(LocalDateTime.now());
        return item;
    }

    private PaymentClient.PaymentResponse paymentResponse(String status) {
        return PaymentClient.PaymentResponse.builder()
                .transactionId("txn-1")
                .status(status)
                .amount(new BigDecimal("150000"))
                .build();
    }
}
