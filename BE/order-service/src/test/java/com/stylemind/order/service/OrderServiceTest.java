package com.stylemind.order.service;

import com.stylemind.cart.dto.CartItemResponse;
import com.stylemind.cart.dto.CartResponse;
import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.order.dto.CreateOrderRequest;
import com.stylemind.order.dto.OrderResponse;
import com.stylemind.order.entity.CheckoutIdempotency;
import com.stylemind.order.entity.Order;
import com.stylemind.order.entity.OrderItem;
import com.stylemind.order.entity.OrderStatus;
import com.stylemind.order.feign.CartClient;
import com.stylemind.order.feign.NotificationClient;
import com.stylemind.order.feign.PaymentClient;
import com.stylemind.order.feign.ProductClient;
import com.stylemind.order.feign.UserClient;
import com.stylemind.order.repository.CheckoutIdempotencyRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock CheckoutIdempotencyRepository checkoutIdempotencyRepository;
    @Mock CartClient cartClient;
    @Mock PaymentClient paymentClient;
    @Mock ProductClient productClient;
    @Mock UserClient userClient;
    @Mock NotificationClient notificationClient;
    @Mock OrderStatusService orderStatusService;

    @InjectMocks OrderService orderService;

    @Test
    void createOrder_emptyCart_throws() {
        CartResponse emptyCart = new CartResponse();
        emptyCart.setItems(List.of());
        when(checkoutIdempotencyRepository.findByUserIdAndIdempotencyKey("user-1", "idem-1")).thenReturn(Optional.empty());
        when(checkoutIdempotencyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartClient.getCart(any(), any())).thenReturn(ApiResponse.success("ok", emptyCart));

        assertThatThrownBy(() -> orderService.createOrder("user-1", "Bearer tok", "idem-1", codReq()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cart is empty");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_variantPriceUnavailable_throwsBeforeSavingOrder() {
        CartResponse cart = cartWithItems();
        when(checkoutIdempotencyRepository.findByUserIdAndIdempotencyKey("user-1", "idem-1")).thenReturn(Optional.empty());
        when(checkoutIdempotencyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartClient.getCart(any(), any())).thenReturn(ApiResponse.success("ok", cart));
        when(productClient.getVariantSnapshot("var-A"))
                .thenReturn(ApiResponse.success("ok", variantSnapshot("var-A", "0")));

        assertThatThrownBy(() -> orderService.createOrder("user-1", "Bearer tok", "idem-1", codReq()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Valid price is unavailable");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_cod_confirmsImmediatelyAndClearsCart() {
        CartResponse cart = cartWithItems();
        when(checkoutIdempotencyRepository.findByUserIdAndIdempotencyKey("user-1", "idem-1")).thenReturn(Optional.empty());
        when(checkoutIdempotencyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartClient.getCart(any(), any())).thenReturn(ApiResponse.success("ok", cart));
        when(productClient.getVariantSnapshot("var-A"))
                .thenReturn(ApiResponse.success("ok", variantSnapshot("var-A", "150000")));
        when(orderRepository.save(any())).thenAnswer(inv -> savedOrder(inv.getArgument(0)));
        when(orderItemRepository.save(any())).thenAnswer(inv -> savedItem(inv.getArgument(0)));
        when(paymentClient.createCodPayment(any())).thenReturn(ApiResponse.success("ok", paymentResponse("PENDING")));
        when(orderStatusService.changeStatus(any(Order.class), eq(OrderStatus.CONFIRMED), eq("user-1")))
                .thenAnswer(inv -> withStatus(inv.getArgument(0), OrderStatus.CONFIRMED));
        when(cartClient.clearCart(any())).thenReturn(ApiResponse.success("ok", null));

        OrderResponse result = orderService.createOrder("user-1", "Bearer tok", "idem-1", codReq());

        assertThat(result.getOrderStatus()).isEqualTo("CONFIRMED");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("150000");
        assertThat(result.getItems().get(0).getPriceAtPurchase()).isEqualByComparingTo("150000");
        verify(paymentClient).createCodPayment(argThat(r ->
                r.getOrderId() != null
                        && "150000".equals(r.getAmount().stripTrailingZeros().toPlainString())
        ));
        verify(paymentClient, never()).createSepayPayment(any());
        verify(cartClient).clearCart("Bearer tok");
    }

    @Test
    void createOrder_cod_notificationFailureDoesNotRollBackOrder() {
        CartResponse cart = cartWithItems();
        when(checkoutIdempotencyRepository.findByUserIdAndIdempotencyKey("user-1", "idem-1")).thenReturn(Optional.empty());
        when(checkoutIdempotencyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartClient.getCart(any(), any())).thenReturn(ApiResponse.success("ok", cart));
        when(productClient.getVariantSnapshot("var-A"))
                .thenReturn(ApiResponse.success("ok", variantSnapshot("var-A", "150000")));
        when(orderRepository.save(any())).thenAnswer(inv -> savedOrder(inv.getArgument(0)));
        when(orderItemRepository.save(any())).thenAnswer(inv -> savedItem(inv.getArgument(0)));
        when(paymentClient.createCodPayment(any())).thenReturn(ApiResponse.success("ok", paymentResponse("PENDING")));
        when(orderStatusService.changeStatus(any(Order.class), eq(OrderStatus.CONFIRMED), eq("user-1")))
                .thenAnswer(inv -> withStatus(inv.getArgument(0), OrderStatus.CONFIRMED));
        when(cartClient.clearCart(any())).thenReturn(ApiResponse.success("ok", null));
        when(userClient.getUserEmail("user-1")).thenThrow(new RuntimeException("auth-service unreachable"));

        OrderResponse result = orderService.createOrder("user-1", "Bearer tok", "idem-1", codReq());

        assertThat(result.getOrderStatus()).isEqualTo("CONFIRMED");
        verify(cartClient).clearCart("Bearer tok");
    }

    @Test
    void createOrder_sepay_createsPendingPaymentTransactionWithQrPayloadAndDoesNotClearCart() {
        CartResponse cart = cartWithItems();
        when(checkoutIdempotencyRepository.findByUserIdAndIdempotencyKey("user-1", "idem-1")).thenReturn(Optional.empty());
        when(checkoutIdempotencyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartClient.getCart(any(), any())).thenReturn(ApiResponse.success("ok", cart));
        when(productClient.getVariantSnapshot("var-A"))
                .thenReturn(ApiResponse.success("ok", variantSnapshot("var-A", "150000")));
        when(orderRepository.save(any())).thenAnswer(inv -> savedOrder(inv.getArgument(0)));
        when(orderItemRepository.save(any())).thenAnswer(inv -> savedItem(inv.getArgument(0)));
        when(paymentClient.createSepayPayment(any())).thenReturn(ApiResponse.success("ok", paymentResponse("PENDING")));

        OrderResponse result = orderService.createOrder("user-1", "Bearer tok", "idem-1", sepayReq());

        assertThat(result.getOrderStatus()).isEqualTo("PAYMENT_PENDING");
        assertThat(result.getPaymentTransactionId()).isEqualTo("txn-1");
        assertThat(result.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(result.getQrImageUrl()).isEqualTo("https://img.vietqr.io/fake");
        assertThat(result.getTransferContent()).isEqualTo("STYLEMIND ORDorder-generated-id");
        verify(paymentClient).createSepayPayment(argThat(r ->
                r.getOrderId() != null
                        && "150000".equals(r.getAmount().stripTrailingZeros().toPlainString())
        ));
        verify(paymentClient, never()).createCodPayment(any());
        verify(cartClient, never()).clearCart(any());
        verify(orderStatusService, never()).changeStatus(any(Order.class), any(), any());
    }

    @Test
    void createOrder_paymentInitFailure_cancelsOrder() {
        CartResponse cart = cartWithItems();
        when(checkoutIdempotencyRepository.findByUserIdAndIdempotencyKey("user-1", "idem-1")).thenReturn(Optional.empty());
        when(checkoutIdempotencyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartClient.getCart(any(), any())).thenReturn(ApiResponse.success("ok", cart));
        when(productClient.getVariantSnapshot("var-A"))
                .thenReturn(ApiResponse.success("ok", variantSnapshot("var-A", "150000")));
        when(orderRepository.save(any())).thenAnswer(inv -> savedOrder(inv.getArgument(0)));
        when(orderItemRepository.save(any())).thenAnswer(inv -> savedItem(inv.getArgument(0)));
        when(paymentClient.createSepayPayment(any())).thenThrow(new RuntimeException("payment-service unreachable"));

        assertThatThrownBy(() -> orderService.createOrder("user-1", "Bearer tok", "idem-1", sepayReq()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unable to initialize payment");

        verify(orderStatusService).changeStatus(any(Order.class), eq(OrderStatus.CANCELLED), eq("user-1"));
    }

    @Test
    void updateOrderStatusFromPayment_paid_movesOnlyToPaidAndClearsCartByUserId() {
        Order order = pendingPaymentOrder();
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderStatusService.changeStatus(any(Order.class), eq(OrderStatus.PAID), eq("PAYMENT_WEBHOOK")))
                .thenAnswer(inv -> withStatus(inv.getArgument(0), OrderStatus.PAID));
        when(cartClient.clearCartByUserId("user-1")).thenReturn(ApiResponse.success("ok", null));

        orderService.updateOrderStatusFromPayment("order-1", "PAID");

        verify(orderStatusService).changeStatus(any(Order.class), eq(OrderStatus.PAID), eq("PAYMENT_WEBHOOK"));
        verify(orderStatusService, never()).changeStatus(any(Order.class), eq(OrderStatus.PROCESSING), eq("PAYMENT_WEBHOOK"));
        verify(cartClient).clearCartByUserId("user-1");
    }

    @Test
    void updateOrderStatusFromPayment_failed_movesToFailedWithoutClearingCart() {
        Order order = pendingPaymentOrder();
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderStatusService.changeStatus(any(Order.class), eq(OrderStatus.FAILED), eq("PAYMENT_WEBHOOK")))
                .thenAnswer(inv -> withStatus(inv.getArgument(0), OrderStatus.FAILED));

        orderService.updateOrderStatusFromPayment("order-1", "FAILED");

        verify(orderStatusService).changeStatus(any(Order.class), eq(OrderStatus.FAILED), eq("PAYMENT_WEBHOOK"));
        verify(cartClient, never()).clearCartByUserId(any());
    }

    @Test
    void updateOrderStatusFromPayment_orderNotPaymentPending_isIdempotentNoOp() {
        Order order = pendingPaymentOrder();
        order.setOrderStatus(OrderStatus.FAILED);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        orderService.updateOrderStatusFromPayment("order-1", "PAID");

        verify(orderStatusService, never()).changeStatus(any(Order.class), any(), any());
        verify(cartClient, never()).clearCartByUserId(any());
    }

    @Test
    void cancelOrder_pendingCancelsWithoutTouchingPaymentService() {
        Order order = Order.builder()
                .id("order-1")
                .userId("user-1")
                .totalAmount(new BigDecimal("150000"))
                .orderStatus(OrderStatus.PENDING)
                .shippingAddress("123 Main Street")
                .build();
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        when(orderRepository.findByIdAndUserId("order-1", "user-1")).thenReturn(Optional.of(order));
        when(orderStatusService.changeStatus(any(Order.class), eq(OrderStatus.CANCELLED), eq("user-1")))
                .thenAnswer(inv -> withStatus(inv.getArgument(0), OrderStatus.CANCELLED));
        when(orderItemRepository.findByOrderId("order-1")).thenReturn(List.of());

        OrderResponse response = orderService.cancelOrder("user-1", "order-1");

        assertThat(response.getOrderStatus()).isEqualTo("CANCELLED");
        verify(paymentClient, never()).expirePaymentByOrderId(any());
    }

    @Test
    void cancelOrder_paymentPending_expiresPaymentBeforeCancellingOrder() {
        Order order = pendingPaymentOrder();
        when(orderRepository.findByIdAndUserId("order-1", "user-1")).thenReturn(Optional.of(order));
        when(orderStatusService.changeStatus(any(Order.class), eq(OrderStatus.CANCELLED), eq("user-1")))
                .thenAnswer(inv -> withStatus(inv.getArgument(0), OrderStatus.CANCELLED));
        when(orderItemRepository.findByOrderId("order-1")).thenReturn(List.of());
        when(paymentClient.expirePaymentByOrderId("order-1")).thenReturn(ApiResponse.success("ok", null));

        OrderResponse response = orderService.cancelOrder("user-1", "order-1");

        assertThat(response.getOrderStatus()).isEqualTo("CANCELLED");
        verify(paymentClient).expirePaymentByOrderId("order-1");
    }

    @Test
    void cancelOrder_paymentExpiryFailure_doesNotCancelOrder() {
        Order order = pendingPaymentOrder();
        when(orderRepository.findByIdAndUserId("order-1", "user-1")).thenReturn(Optional.of(order));
        when(paymentClient.expirePaymentByOrderId("order-1"))
                .thenThrow(new RuntimeException("payment service unavailable"));

        assertThatThrownBy(() -> orderService.cancelOrder("user-1", "order-1"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus()).isEqualTo(502))
                .hasMessageContaining("Không thể hủy thanh toán");

        verify(orderStatusService, never()).changeStatus(any(Order.class), eq(OrderStatus.CANCELLED), any());
    }

    @Test
    void cancelOrder_paidStatus_isRejected() {
        Order order = Order.builder()
                .id("order-1")
                .userId("user-1")
                .totalAmount(new BigDecimal("150000"))
                .orderStatus(OrderStatus.PAID)
                .shippingAddress("123 Main Street")
                .build();
        when(orderRepository.findByIdAndUserId("order-1", "user-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("user-1", "order-1"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus()).isEqualTo(409))
                .hasMessageContaining("Không thể hủy thanh toán");

        verify(orderStatusService, never()).changeStatus(any(Order.class), any(), any());
        verify(paymentClient, never()).expirePaymentByOrderId(any());
    }

    @Test
    void createOrder_sameIdempotencyKey_returnsExistingOrderWithoutCreatingNewPayment() {
        CheckoutIdempotency checkout = CheckoutIdempotency.builder()
                .id("checkout-1")
                .userId("user-1")
                .idempotencyKey("idem-1")
                .orderId("order-1")
                .status("SUCCEEDED")
                .build();
        Order order = pendingPaymentOrder();

        when(checkoutIdempotencyRepository.findByUserIdAndIdempotencyKey("user-1", "idem-1"))
                .thenReturn(Optional.of(checkout));
        when(orderRepository.findByIdAndUserId("order-1", "user-1")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId("order-1")).thenReturn(List.of());
        when(paymentClient.getPaymentStatus("order-1")).thenReturn(ApiResponse.success("ok", paymentResponse("PENDING")));

        OrderResponse response = orderService.createOrder("user-1", "Bearer tok", "idem-1", sepayReq());

        assertThat(response.getId()).isEqualTo("order-1");
        verify(paymentClient, never()).createSepayPayment(any());
        verify(paymentClient, never()).createCodPayment(any());
        verify(cartClient, never()).getCart(any(), any());
    }

    private Order withStatus(Order order, OrderStatus status) {
        order.setOrderStatus(status);
        return order;
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

    private CreateOrderRequest sepayReq() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setShippingAddress("123 Main Street");
        req.setPaymentMethod("sepay");
        return req;
    }

    private Order pendingPaymentOrder() {
        Order order = Order.builder()
                .id("order-1")
                .userId("user-1")
                .totalAmount(new BigDecimal("150000"))
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .shippingAddress("123 Main Street")
                .build();
        return savedOrder(order);
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
                .qrImageUrl("https://img.vietqr.io/fake")
                .qrContent("970436|0123456789|150000|STYLEMIND ORDorder-generated-id")
                .transferContent("STYLEMIND ORDorder-generated-id")
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
