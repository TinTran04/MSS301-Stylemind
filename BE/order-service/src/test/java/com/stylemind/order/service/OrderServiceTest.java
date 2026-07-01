package com.stylemind.order.service;

import com.stylemind.cart.dto.CartItemResponse;
import com.stylemind.cart.dto.CartResponse;
import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.order.dto.ConfirmPaymentRequest;
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

    @Test
    void createOrder_emptyCart_throws() {
        CartResponse emptyCart = new CartResponse();
        emptyCart.setItems(List.of());
        when(cartClient.getCart(any(), any())).thenReturn(ApiResponse.success("ok", emptyCart));

        assertThatThrownBy(() -> orderService.createOrder("user-1", "Bearer tok", codReq()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cart is empty");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_variantPriceUnavailable_throwsBeforeSavingOrder() {
        CartResponse cart = cartWithItems();
        when(cartClient.getCart(any(), any())).thenReturn(ApiResponse.success("ok", cart));
        when(productClient.getVariantSnapshot("var-A"))
                .thenReturn(ApiResponse.success("ok", variantSnapshot("var-A", "0")));

        assertThatThrownBy(() -> orderService.createOrder("user-1", "Bearer tok", codReq()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Valid price is unavailable");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_cod_pendingWithoutPaymentCallAndClearsCart() {
        CartResponse cart = cartWithItems();
        when(cartClient.getCart(any(), any())).thenReturn(ApiResponse.success("ok", cart));
        when(productClient.getVariantSnapshot("var-A"))
                .thenReturn(ApiResponse.success("ok", variantSnapshot("var-A", "150000")));
        when(orderRepository.save(any())).thenAnswer(inv -> savedOrder(inv.getArgument(0)));
        when(orderItemRepository.save(any())).thenAnswer(inv -> savedItem(inv.getArgument(0)));
        when(cartClient.clearCart(any())).thenReturn(ApiResponse.success("ok", null));

        OrderResponse result = orderService.createOrder("user-1", "Bearer tok", codReq());

        assertThat(result.getOrderStatus()).isEqualTo("PENDING");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("150000");
        assertThat(result.getItems().get(0).getPriceAtPurchase()).isEqualByComparingTo("150000");
        verify(paymentClient, never()).checkout(any());
        verify(paymentClient, never()).processPayment(any());
        verify(cartClient).clearCart("Bearer tok");
    }

    @Test
    void createOrder_onlineSimulated_createsPendingPaymentTransaction() {
        CartResponse cart = cartWithItems();
        when(cartClient.getCart(any(), any())).thenReturn(ApiResponse.success("ok", cart));
        when(productClient.getVariantSnapshot("var-A"))
                .thenReturn(ApiResponse.success("ok", variantSnapshot("var-A", "150000")));
        when(orderRepository.save(any())).thenAnswer(inv -> savedOrder(inv.getArgument(0)));
        when(orderItemRepository.save(any())).thenAnswer(inv -> savedItem(inv.getArgument(0)));
        when(paymentClient.checkout(any())).thenReturn(ApiResponse.success("ok", paymentResponse("PENDING")));

        OrderResponse result = orderService.createOrder("user-1", "Bearer tok", onlineReq());

        assertThat(result.getOrderStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(result.getPaymentTransactionId()).isEqualTo("txn-1");
        assertThat(result.getPaymentStatus()).isEqualTo("PENDING");
        verify(paymentClient).checkout(argThat(r ->
                "online_simulated".equals(r.getMethod())
                        && "150000".equals(r.getAmount().stripTrailingZeros().toPlainString())
        ));
        verify(paymentClient, never()).processPayment(any());
        verify(cartClient, never()).clearCart(any());
    }

    @Test
    void confirmOnlinePayment_success_updatesOrderToProcessingAndClearsCart() {
        Order order = pendingPaymentOrder();
        when(orderRepository.findByIdAndUserId("order-1", "user-1")).thenReturn(Optional.of(order));
        when(paymentClient.processPayment(any())).thenReturn(ApiResponse.success("ok", paymentResponse("COMPLETED")));
        when(orderRepository.save(any())).thenAnswer(inv -> savedOrder(inv.getArgument(0)));
        when(orderItemRepository.findByOrderId("order-1")).thenReturn(List.of(savedItem(orderItem())));
        when(cartClient.clearCart(any())).thenReturn(ApiResponse.success("ok", null));

        OrderResponse result = orderService.confirmOnlinePayment("user-1", "Bearer tok", "order-1", confirmReq("123456"));

        assertThat(result.getOrderStatus()).isEqualTo("PROCESSING");
        assertThat(result.getPaymentStatus()).isEqualTo("COMPLETED");
        verify(paymentClient).processPayment(argThat(r ->
                "txn-1".equals(r.getTransactionId()) && "123456".equals(r.getVerificationCode())
        ));
        verify(cartClient).clearCart("Bearer tok");
    }

    @Test
    void confirmOnlinePayment_failed_updatesOrderToCancelledWithoutClearingCart() {
        Order order = pendingPaymentOrder();
        when(orderRepository.findByIdAndUserId("order-1", "user-1")).thenReturn(Optional.of(order));
        when(paymentClient.processPayment(any())).thenReturn(ApiResponse.success("ok", paymentResponse("FAILED")));
        when(orderRepository.save(any())).thenAnswer(inv -> savedOrder(inv.getArgument(0)));
        when(orderItemRepository.findByOrderId("order-1")).thenReturn(List.of(savedItem(orderItem())));

        OrderResponse result = orderService.confirmOnlinePayment("user-1", "Bearer tok", "order-1", confirmReq("000000"));

        assertThat(result.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(result.getPaymentStatus()).isEqualTo("FAILED");
        verify(cartClient, never()).clearCart(any());
    }

    private CartResponse cartWithItems() {
        CartItemResponse item = new CartItemResponse();
        item.setVariantId("var-A");
        item.setQuantity(1);
        item.setIsAiRecommended(false);

        CartResponse cart = new CartResponse();
        cart.setItems(List.of(item));
        cart.setTotalAmount(BigDecimal.ZERO);
        return cart;
    }

    private CreateOrderRequest codReq() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setShippingAddress("123 Main Street");
        req.setPaymentMethod("cod");
        return req;
    }

    private CreateOrderRequest onlineReq() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setShippingAddress("123 Main Street");
        req.setPaymentMethod("online_simulated");
        return req;
    }

    private ConfirmPaymentRequest confirmReq(String verificationCode) {
        ConfirmPaymentRequest req = new ConfirmPaymentRequest();
        req.setTransactionId("txn-1");
        req.setVerificationCode(verificationCode);
        return req;
    }

    private Order pendingPaymentOrder() {
        Order order = Order.builder()
                .id("order-1")
                .userId("user-1")
                .totalAmount(new BigDecimal("150000"))
                .orderStatus("PENDING_PAYMENT")
                .shippingAddress("123 Main Street")
                .build();
        return savedOrder(order);
    }

    private OrderItem orderItem() {
        return OrderItem.builder()
                .id("item-1")
                .orderId("order-1")
                .variantId("var-A")
                .quantity(1)
                .priceAtPurchase(new BigDecimal("150000"))
                .isAiConversion(false)
                .build();
    }

    private Order savedOrder(Order order) {
        if (order.getId() == null) order.setId("order-generated-id");
        if (order.getCreatedAt() == null) order.setCreatedAt(LocalDateTime.now());
        if (order.getUpdatedAt() == null) order.setUpdatedAt(LocalDateTime.now());
        return order;
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

    private ProductClient.VariantSnapshot variantSnapshot(String variantId, String price) {
        ProductClient.VariantSnapshot snapshot = new ProductClient.VariantSnapshot();
        snapshot.setVariantId(variantId);
        snapshot.setProductId("prod-1");
        snapshot.setProductName("Product 1");
        snapshot.setEffectivePrice(new BigDecimal(price));
        snapshot.setStatus("ACTIVE");
        return snapshot;
    }
}
