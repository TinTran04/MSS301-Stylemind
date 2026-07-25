package com.stylemind.order.service;

import com.stylemind.cart.dto.CartItemResponse;
import com.stylemind.cart.dto.CartResponse;
import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.order.dto.CreateOrderRequest;
import com.stylemind.order.entity.Order;
import com.stylemind.order.feign.CartClient;
import com.stylemind.order.feign.PaymentClient;
import com.stylemind.order.feign.ProductClient;
import com.stylemind.order.feign.UserAddressClient;
import com.stylemind.order.repository.CheckoutIdempotencyRepository;
import com.stylemind.order.repository.OrderItemRepository;
import com.stylemind.order.repository.OrderRepository;
import com.stylemind.order.repository.OrderStatusAuditLogRepository;
import com.stylemind.order.repository.OrderDeliveryImageRepository;
import com.stylemind.order.feign.NotificationClient;
import com.stylemind.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderAddressCheckoutTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock CheckoutIdempotencyRepository checkoutIdempotencyRepository;
    @Mock CartClient cartClient;
    @Mock PaymentClient paymentClient;
    @Mock ProductClient productClient;
    @Mock com.stylemind.order.feign.UserClient userClient;
    @Mock UserAddressClient userAddressClient;
    @Mock NotificationClient notificationClient;
    @Mock OrderStatusAuditLogRepository auditLogRepository;
    @Mock OrderStatusService orderStatusService;
    @Mock OrderDeliveryImageRepository deliveryImageRepository;

    @InjectMocks OrderServiceImpl orderService;

    @BeforeEach
    void setUpIdempotency() {
        when(checkoutIdempotencyRepository.findByUserIdAndIdempotencyKey(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(checkoutIdempotencyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void invalidAddressStopsBeforeCartOrPayment() {
        when(userAddressClient.getAddress("user-1", "legacy-1"))
                .thenThrow(new RuntimeException("address rejected"));

        assertThatThrownBy(() -> orderService.createOrder("user-1", "Bearer token", "key-1", request("legacy-1")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("xác thực địa chỉ");

        verify(cartClient, never()).getCart(any(), any());
        verify(paymentClient, never()).createCodPayment(any());
        verify(paymentClient, never()).createSepayPayment(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void validAddressIsPassedToCheckoutBeforeOrderPersistence() {
        UserAddressClient.DeliveryAddressSnapshot address = new UserAddressClient.DeliveryAddressSnapshot();
        address.setId("address-1");
        address.setUserId("user-1");
        address.setValidationStatus("VALID");
        address.setRecipientName("Test User");
        address.setPhoneNumber("+84912345678");
        address.setProvinceCode("01");
        address.setProvinceName("Thành phố Hà Nội");
        address.setWardCode("00004");
        address.setWardName("Phường Ba Đình");
        address.setAddressLine("Số nhà 1");
        when(userAddressClient.getAddress("user-1", "address-1"))
                .thenReturn(ApiResponse.success("ok", address));

        CartResponse cart = new CartResponse();
        CartItemResponse item = new CartItemResponse();
        item.setVariantId("variant-1");
        item.setQuantity(1);
        cart.setItems(List.of(item));
        when(cartClient.getCart(any(), any())).thenReturn(ApiResponse.success("ok", cart));
        ProductClient.VariantSnapshot variant = new ProductClient.VariantSnapshot();
        variant.setVariantId("variant-1");
        variant.setEffectivePrice(new BigDecimal("100000"));
        variant.setStatus("ACTIVE");
        when(productClient.getVariantSnapshot("variant-1")).thenReturn(ApiResponse.success("ok", variant));

        Order persistedOrder = Order.builder().id("order-1").userId("user-1")
                .totalAmount(new BigDecimal("100000"))
                .orderStatus(com.stylemind.order.entity.OrderStatus.PENDING)
                .shippingAddress("Số nhà 1, Phường Ba Đình, Thành phố Hà Nội")
                .build();
        persistedOrder.setCreatedAt(java.time.LocalDateTime.now());
        persistedOrder.setUpdatedAt(java.time.LocalDateTime.now());
        when(orderRepository.save(any(Order.class))).thenReturn(persistedOrder);

        when(orderItemRepository.save(any())).thenAnswer(invocation -> {
            com.stylemind.order.entity.OrderItem saved = invocation.getArgument(0);
            saved.setCreatedAt(java.time.LocalDateTime.now());
            saved.setUpdatedAt(java.time.LocalDateTime.now());
            return saved;
        });
        when(orderStatusService.changeStatus(any(Order.class), eq(com.stylemind.order.entity.OrderStatus.CONFIRMED), eq("user-1")))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentClient.createCodPayment(any()))
                .thenReturn(ApiResponse.success("ok", com.stylemind.order.feign.PaymentClient.PaymentResponse.builder()
                        .transactionId("payment-1")
                        .status("PENDING")
                        .method("COD")
                        .build()));

        orderService.createOrder("user-1", "Bearer token", "key-1", request("address-1"));

        verify(userAddressClient).getAddress("user-1", "address-1");
        verify(cartClient).getCart("Bearer token", null);
        verify(orderRepository).save(argThat(order ->
                "address-1".equals(order.getSourceAddressId())
                        && "+84912345678".equals(order.getShippingPhone())
                        && "01".equals(order.getShippingProvinceCode())
                        && "00004".equals(order.getShippingWardCode())
                        && "Số nhà 1".equals(order.getShippingAddressLine())));
    }

    private CreateOrderRequest request(String addressId) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAddressId(addressId);
        request.setPaymentMethod("cod");
        return request;
    }
}
