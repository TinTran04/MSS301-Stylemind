package com.stylemind.order.service;

import com.stylemind.cart.dto.CartItemResponse;
import com.stylemind.cart.dto.CartResponse;
import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.dto.PageResponse;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.order.dto.AdminOrderSummaryResponse;
import com.stylemind.order.dto.AdminOrdersResponse;
import com.stylemind.order.dto.CreateOrderRequest;
import com.stylemind.order.dto.OrderItemCountResponse;
import com.stylemind.order.dto.OrderSummaryResponse;
import com.stylemind.order.dto.OrderResponse;
import com.stylemind.order.entity.CheckoutIdempotency;
import com.stylemind.order.entity.Order;
import com.stylemind.order.entity.OrderItem;
import com.stylemind.order.entity.OrderReturnStatus;
import com.stylemind.order.entity.OrderStatus;
import com.stylemind.order.entity.OrderStatusAuditLog;
import com.stylemind.order.feign.CartClient;
import com.stylemind.order.feign.PaymentClient;
import com.stylemind.order.feign.ProductClient;
import com.stylemind.order.feign.UserClient;
import com.stylemind.order.feign.UserAddressClient;
import com.stylemind.order.repository.CheckoutIdempotencyRepository;
import com.stylemind.order.repository.OrderItemRepository;
import com.stylemind.order.repository.OrderRepository;
import com.stylemind.order.repository.OrderDeliveryImageRepository;
import com.stylemind.order.repository.OrderReturnAttachmentRepository;
import com.stylemind.order.repository.OrderReturnRequestRepository;
import com.stylemind.order.repository.OrderStatusAuditLogRepository;
import com.stylemind.order.service.impl.OrderServiceImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    @Mock UserAddressClient userAddressClient;
    @Mock OrderStatusAuditLogRepository auditLogRepository;
    @Mock OrderStatusService orderStatusService;
    @Mock OrderDeliveryImageRepository deliveryImageRepository;
    @Mock OrderReturnRequestRepository returnRequestRepository;
    @Mock OrderReturnAttachmentRepository returnAttachmentRepository;

    @InjectMocks OrderServiceImpl orderService;

    @BeforeEach
    void stubCheckoutAddress() {
        UserAddressClient.DeliveryAddressSnapshot address = new UserAddressClient.DeliveryAddressSnapshot();
        address.setId("address-1");
        address.setUserId("user-1");
        address.setRecipientName("Test User");
        address.setPhoneNumber("+84901234567");
        address.setProvinceCode("01");
        address.setProvinceName("Thành phố Hà Nội");
        address.setWardCode("00004");
        address.setWardName("Phường Ba Đình");
        address.setAddressLine("123 Main Street");
        address.setValidationStatus("VALID");
        lenient().when(userAddressClient.getAddress(anyString(), anyString()))
                .thenReturn(ApiResponse.success("ok", address));
        lenient().when(returnRequestRepository.findByOrderIdOrderByCreatedAtDesc(anyString()))
                .thenReturn(List.of());
    }

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
    void getOrderForAdmin_enrichesCustomerVariantAndPaymentDetailsWithoutChangingPurchasePrice() {
        Order order = pendingPaymentOrder();
        OrderItem item = savedItem(OrderItem.builder()
                .id("item-1")
                .orderId("order-1")
                .variantId("var-A")
                .quantity(2)
                .priceAtPurchase(new BigDecimal("150000"))
                .build());
        PaymentClient.PaymentResponse payment = PaymentClient.PaymentResponse.builder()
                .transactionId("txn-1")
                .method("sepay")
                .transactionRef("SEVQR STYLEMIND SMABC1234")
                .gatewayTransactionId("gateway-1")
                .status("PAID")
                .amount(new BigDecimal("300000"))
                .paidAt(Instant.parse("2026-07-19T10:15:30Z"))
                .build();
        UserClient.UserEmail userEmail = new UserClient.UserEmail();
        userEmail.setUserId("user-1");
        userEmail.setEmail("customer@example.com");

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId("order-1")).thenReturn(List.of(item));
        when(userClient.getUserEmail("user-1")).thenReturn(ApiResponse.success("ok", userEmail));
        when(paymentClient.getPaymentStatus("order-1")).thenReturn(ApiResponse.success("ok", payment));
        when(productClient.getVariantSnapshot("var-A"))
                .thenReturn(ApiResponse.success("ok", variantSnapshotWithDetails()));
        when(auditLogRepository.findByOrderIdOrderByCreatedAtAsc("order-1"))
                .thenReturn(List.of(OrderStatusAuditLog.builder()
                        .id("audit-1")
                        .orderId("order-1")
                        .actorId("admin-1")
                        .fromStatus(OrderStatus.PAID)
                        .toStatus(OrderStatus.PROCESSING)
                        .build()));

        OrderResponse response = orderService.getOrderForAdmin("order-1");

        assertThat(response.getCustomerEmail()).isEqualTo("customer@example.com");
        assertThat(response.getPaymentMethod()).isEqualTo("sepay");
        assertThat(response.getPaymentReference()).isEqualTo("SEVQR STYLEMIND SMABC1234");
        assertThat(response.getGatewayTransactionId()).isEqualTo("gateway-1");
        assertThat(response.getPaidAt()).isEqualTo(Instant.parse("2026-07-19T10:15:30Z"));
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getPriceAtPurchase()).isEqualByComparingTo("150000");
        assertThat(response.getItems().get(0).getProductName()).isEqualTo("Oxford Shirt");
        assertThat(response.getItems().get(0).getCatalogVariantId()).isEqualTo("var-A");
        assertThat(response.getItems().get(0).getProductId()).isEqualTo("prod-1");
        assertThat(response.getItems().get(0).getSku()).isEqualTo("SHIRT-WHITE-M");
        assertThat(response.getItems().get(0).getColor()).isEqualTo("Trắng");
        assertThat(response.getItems().get(0).getSize()).isEqualTo("M");
        assertThat(response.getStatusHistory()).hasSize(1);
        assertThat(response.getStatusHistory().get(0).getPreviousStatus()).isEqualTo("PAID");
        assertThat(response.getStatusHistory().get(0).getNewStatus()).isEqualTo("PROCESSING");
        assertThat(response.getStatusHistory().get(0).getActor()).isEqualTo("admin-1");
    }

    @Test
    void getOrdersPage_returnsDatabasePageSummaryForPrincipalUserWithoutDetailEnrichment() {
        Order order = savedOrder(Order.builder()
                .id("order-1")
                .userId("user-1")
                .totalAmount(new BigDecimal("399000"))
                .orderStatus(OrderStatus.PROCESSING)
                .shippingAddress("123 Main Street")
                .build());
        PageRequest pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        when(orderRepository.findByUserId("user-1", pageable))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 25));
        when(orderItemRepository.countByOrderIds(List.of("order-1")))
                .thenReturn(List.of(new OrderItemCountResponse("order-1", 3L)));

        PageResponse<OrderSummaryResponse> response = orderService.getOrdersPage("user-1", pageable);

        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(25);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getId()).isEqualTo("order-1");
        assertThat(response.getContent().get(0).getItemCount()).isEqualTo(3);
        assertThat(response.getContent().get(0).getOrderStatus()).isEqualTo("PROCESSING");
        verify(orderRepository).findByUserId("user-1", pageable);
        verify(orderItemRepository, never()).findByOrderId(anyString());
        verifyNoInteractions(paymentClient, productClient, auditLogRepository, deliveryImageRepository);
    }

    @Test
    void getOrdersPage_returnsEmptyPageMetadataWithoutItemCountQuery() {
        PageRequest pageable = PageRequest.of(
                4,
                10,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        when(orderRepository.findByUserId("user-1", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<OrderSummaryResponse> response = orderService.getOrdersPage("user-1", pageable);

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getPage()).isEqualTo(4);
        assertThat(response.getTotalElements()).isZero();
        assertThat(response.getTotalPages()).isZero();
        assertThat(response.isEmpty()).isTrue();
        verify(orderItemRepository, never()).countByOrderIds(any());
    }

    @Test
    void getOrdersPage_appliesAuthenticatedUserAndStatusFilterInRepositoryQuery() {
        PageRequest pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        when(orderRepository.findByUserIdAndOrderStatus("user-1", OrderStatus.PROCESSING, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<OrderSummaryResponse> response =
                orderService.getOrdersPage("user-1", "processing", pageable);

        assertThat(response.isEmpty()).isTrue();
        verify(orderRepository).findByUserIdAndOrderStatus("user-1", OrderStatus.PROCESSING, pageable);
        verify(orderRepository, never()).findByUserId("user-1", pageable);
    }

    @Test
    void getOrdersPage_rejectsUnknownStatusFilter() {
        PageRequest pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> orderService.getOrdersPage("user-1", "not-a-status", pageable))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "INVALID_ORDER_STATUS_FILTER");
        verifyNoInteractions(orderRepository);
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
        assertThat(result.getSubtotalAmount()).isEqualByComparingTo("150000");
        assertThat(result.getShippingFee()).isEqualByComparingTo("15000");
        assertThat(result.getTaxAmount()).isEqualByComparingTo("15000");
        assertThat(result.getRoundingAdjustment()).isEqualByComparingTo("0");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("180000");
        assertThat(result.getItems().get(0).getPriceAtPurchase()).isEqualByComparingTo("150000");
        verify(paymentClient).createCodPayment(argThat(r ->
                r.getOrderId() != null
                        && "180000".equals(r.getAmount().stripTrailingZeros().toPlainString())
        ));
        verify(paymentClient, never()).createSepayPayment(any());
        verify(cartClient).clearCart("Bearer tok");
    }

    @Test
    void createOrder_codStoresExactPricingBreakdownWithoutCashRounding() {
        CartResponse cart = cartWithItems();
        when(checkoutIdempotencyRepository.findByUserIdAndIdempotencyKey("user-1", "idem-1")).thenReturn(Optional.empty());
        when(checkoutIdempotencyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartClient.getCart(any(), any())).thenReturn(ApiResponse.success("ok", cart));
        when(productClient.getVariantSnapshot("var-A"))
                .thenReturn(ApiResponse.success("ok", variantSnapshot("var-A", "1113000")));
        when(orderRepository.save(any())).thenAnswer(inv -> savedOrder(inv.getArgument(0)));
        when(orderItemRepository.save(any())).thenAnswer(inv -> savedItem(inv.getArgument(0)));
        when(paymentClient.createCodPayment(any())).thenReturn(ApiResponse.success("ok", paymentResponse("PENDING")));
        when(orderStatusService.changeStatus(any(Order.class), eq(OrderStatus.CONFIRMED), eq("user-1")))
                .thenAnswer(inv -> withStatus(inv.getArgument(0), OrderStatus.CONFIRMED));
        when(cartClient.clearCart(any())).thenReturn(ApiResponse.success("ok", null));

        OrderResponse result = orderService.createOrder("user-1", "Bearer tok", "idem-1", codReq());

        assertThat(result.getSubtotalAmount()).isEqualByComparingTo("1113000");
        assertThat(result.getShippingFee()).isEqualByComparingTo("0");
        assertThat(result.getTaxAmount()).isEqualByComparingTo("111300");
        assertThat(result.getRoundingAdjustment()).isEqualByComparingTo("0");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("1224300");
        verify(paymentClient).createCodPayment(argThat(r ->
                r.getOrderId() != null
                        && "1224300".equals(r.getAmount().stripTrailingZeros().toPlainString())
        ));
    }

    @Test
    void createOrder_cod_confirmsEvenThoughNotificationIsDeferredAfterCommit() {
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
        assertThat(result.getSubtotalAmount()).isEqualByComparingTo("150000");
        assertThat(result.getShippingFee()).isEqualByComparingTo("15000");
        assertThat(result.getTaxAmount()).isEqualByComparingTo("15000");
        assertThat(result.getRoundingAdjustment()).isEqualByComparingTo("0");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("180000");
        assertThat(result.getQrImageUrl()).isEqualTo("https://img.vietqr.io/fake");
        assertThat(result.getTransferContent()).isEqualTo("STYLEMIND ORDorder-generated-id");
        verify(paymentClient).createSepayPayment(argThat(r ->
                r.getOrderId() != null
                        && "180000".equals(r.getAmount().stripTrailingZeros().toPlainString())
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
    void updateOrderStatusFromPayment_paidDoesNotMoveToFailedWhenPostProcessingSucceeds() {
        Order order = pendingPaymentOrder();
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderStatusService.changeStatus(any(Order.class), eq(OrderStatus.PAID), eq("PAYMENT_WEBHOOK")))
                .thenAnswer(inv -> withStatus(inv.getArgument(0), OrderStatus.PAID));
        when(cartClient.clearCartByUserId("user-1")).thenReturn(ApiResponse.success("ok", null));

        orderService.updateOrderStatusFromPayment("order-1", "PAID");

        verify(orderStatusService).changeStatus(any(Order.class), eq(OrderStatus.PAID), eq("PAYMENT_WEBHOOK"));
        verify(orderStatusService, never()).changeStatus(any(Order.class), eq(OrderStatus.FAILED), any());
        verify(orderStatusService, never()).changeStatus(any(Order.class), eq(OrderStatus.CANCELLED), any());
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

    @Test
    void getAdminSummary_countsRevenueOnlyForCompletedOrders() {
        when(orderRepository.sumRevenueByStatusesExcludingReturnStatus(
                argThat(this::containsOnlyCompletedStatus),
                eq(OrderReturnStatus.REFUNDED)))
                .thenReturn(new BigDecimal("250000"));
        when(orderRepository.sumRevenueByStatusesSinceExcludingReturnStatus(
                argThat(this::containsOnlyCompletedStatus),
                any(LocalDateTime.class),
                eq(OrderReturnStatus.REFUNDED)))
                .thenReturn(new BigDecimal("125000"));

        AdminOrderSummaryResponse response = orderService.getAdminSummary();

        assertThat(response.getTotalRevenue()).isEqualByComparingTo("250000");
        assertThat(response.getTodayRevenue()).isEqualByComparingTo("125000");
        verify(orderRepository).sumRevenueByStatusesExcludingReturnStatus(
                argThat(this::containsOnlyCompletedStatus),
                eq(OrderReturnStatus.REFUNDED));
        verify(orderRepository).sumRevenueByStatusesSinceExcludingReturnStatus(
                argThat(this::containsOnlyCompletedStatus),
                any(LocalDateTime.class),
                eq(OrderReturnStatus.REFUNDED));
    }

    @Test
    void getAllOrdersForAdmin_countsRevenueOnlyForCompletedOrders() {
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(orderRepository.search(isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<Order>(List.of(), pageable, 0));
        when(orderRepository.sumRevenueForSearch(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                argThat(this::containsOnlyCompletedStatus),
                eq(OrderReturnStatus.REFUNDED)))
                .thenReturn(new BigDecimal("300000"));

        AdminOrdersResponse response = orderService.getAllOrdersForAdmin(null, null, null, null, null, null, pageable);

        assertThat(response.getTotalRevenue()).isEqualByComparingTo("300000");
        verify(orderRepository).sumRevenueForSearch(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                argThat(this::containsOnlyCompletedStatus),
                eq(OrderReturnStatus.REFUNDED));
    }

    @Test
    void uploadDeliveryImage_requiresCompletedOrder() {
        Order order = savedOrder(Order.builder()
                .id("order-1")
                .userId("user-1")
                .totalAmount(new BigDecimal("177000"))
                .orderStatus(OrderStatus.SHIPPED)
                .shippingAddress("123 Main Street")
                .build());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "received.jpg",
                "image/jpeg",
                new byte[] { 1, 2, 3 }
        );
        when(orderRepository.findByIdAndUserId("order-1", "user-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.uploadDeliveryImage("user-1", "order-1", file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("giao thành công");

        verify(deliveryImageRepository, never()).save(any());
    }

    @Test
    void uploadDeliveryImage_savesProofForCompletedOrderAndReturnsImages() {
        Order order = savedOrder(Order.builder()
                .id("order-1")
                .userId("user-1")
                .totalAmount(new BigDecimal("177000"))
                .orderStatus(OrderStatus.COMPLETED)
                .shippingAddress("123 Main Street")
                .build());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "received.jpg",
                "image/jpeg",
                new byte[] { 1, 2, 3 }
        );

        when(orderRepository.findByIdAndUserId("order-1", "user-1")).thenReturn(Optional.of(order));
        when(deliveryImageRepository.countByOrderId("order-1")).thenReturn(0L);
        when(deliveryImageRepository.save(any())).thenAnswer(invocation -> {
            com.stylemind.order.entity.OrderDeliveryImage image = invocation.getArgument(0);
            image.setCreatedAt(LocalDateTime.parse("2026-07-21T10:00:00"));
            image.setUpdatedAt(LocalDateTime.parse("2026-07-21T10:00:00"));
            return image;
        });
        when(orderItemRepository.findByOrderId("order-1")).thenReturn(List.of());
        when(auditLogRepository.findByOrderIdOrderByCreatedAtAsc("order-1")).thenReturn(List.of());
        when(deliveryImageRepository.findByOrderIdOrderByCreatedAtDesc("order-1"))
                .thenReturn(List.of(com.stylemind.order.entity.OrderDeliveryImage.builder()
                        .id("img-1")
                        .orderId("order-1")
                        .userId("user-1")
                        .fileName("received.jpg")
                        .contentType("image/jpeg")
                        .sizeBytes(3L)
                        .imageData(new byte[] { 1, 2, 3 })
                        .build()));

        OrderResponse response = orderService.uploadDeliveryImage("user-1", "order-1", file);

        assertThat(response.getDeliveryImages()).hasSize(1);
        assertThat(response.getDeliveryImages().get(0).getImageDataUrl()).startsWith("data:image/jpeg;base64,");
        verify(deliveryImageRepository).save(any());
    }

    private boolean containsOnlyCompletedStatus(Collection<OrderStatus> statuses) {
        return statuses != null
                && statuses.size() == 1
                && statuses.contains(OrderStatus.COMPLETED);
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
        req.setAddressId("address-1");
        req.setPaymentMethod("cod");
        return req;
    }

    private CreateOrderRequest sepayReq() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId("address-1");
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

    private ProductClient.VariantSnapshot variantSnapshotWithDetails() {
        ProductClient.VariantSnapshot snapshot = variantSnapshot("var-A", "175000");
        snapshot.setProductName("Oxford Shirt");
        snapshot.setSku("SHIRT-WHITE-M");
        snapshot.setColor("Trắng");
        snapshot.setSize("M");
        snapshot.setMaterial("Cotton");
        snapshot.setPrimaryImageUrl("https://images.example.test/oxford-shirt.jpg");
        return snapshot;
    }
}
