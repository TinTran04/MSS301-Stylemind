package com.stylemind.order.service;

import com.stylemind.cart.dto.CartItemResponse;
import com.stylemind.cart.dto.CartMergeRequest;
import com.stylemind.cart.dto.CartResponse;
import com.stylemind.order.dto.*;
import com.stylemind.order.entity.*;
import com.stylemind.order.repository.*;
import com.stylemind.order.feign.*;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartClient cartClient;
    private final PaymentClient paymentClient;
    private final ProductClient productClient;

    public OrderResponse createOrder(String userId, String authHeader, CreateOrderRequest request) {
        // Get cart
        CartResponse cart = getCart(authHeader).getData();
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessException("CART_EMPTY", "Giỏ hàng trống", 400);
        }

        // Create order with PENDING status
        String orderId = StringUtil.generateUniqueId();
        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .totalAmount(cart.getTotalAmount())
                .orderStatus("PENDING")
                .shippingAddress(request.getShippingAddress())
                .build();

        order = orderRepository.save(order);

        // Create order items
        List<OrderItem> orderItems = cart.getItems().stream().map(cartItem -> {
            OrderItem item = OrderItem.builder()
                    .id(StringUtil.generateUniqueId())
                    .orderId(orderId)
                    .variantId(cartItem.getVariantId())
                    .quantity(cartItem.getQuantity())
                    .priceAtPurchase(getVariantPrice(cartItem))
                    .isAiConversion(cartItem.getIsAiRecommended())
                    .sourceBundleId(cartItem.getSourceBundleId())
                    .build();
            return orderItemRepository.save(item);
        }).collect(Collectors.toList());

        // Process payment (Saga step 2 - no inventory reservation step)
        if ("online_simulated".equals(request.getPaymentMethod())) {
            if (request.getTransactionId() == null) {
                throw new BusinessException("INVALID_PAYMENT", "Thiếu transactionId cho thanh toán online", 400);
            }

            try {
                processPayment(orderId, request.getTransactionId(), order.getTotalAmount());
            } catch (Exception ex) {
                log.error("Payment failed for order: {}", orderId, ex);
                order.setOrderStatus("CANCELLED");
                orderRepository.save(order);
                throw new BusinessException("PAYMENT_FAILED", "Thanh toán thất bại: " + ex.getMessage(), 400);
            }
        }

        // Complete order
        order.setOrderStatus("FULFILLED");
        order = orderRepository.save(order);

        // Clear cart
        cartClient.mergeCart(authHeader, CartMergeRequest.builder()
                .guestSessionId("") // Will be handled by cart service
                .build());

        return buildOrderResponse(order, orderItems);
    }

    private void processPayment(String orderId, String transactionId, BigDecimal amount) {
        PaymentClient.ProcessPaymentRequest paymentRequest = PaymentClient.ProcessPaymentRequest.builder()
                .transactionId(transactionId)
                .orderId(orderId)
                .amount(amount)
                .build();
        paymentClient.processPayment(paymentRequest);
    }

    private String getVariantSku(String variantId) {
        ProductClient.VariantDetail variant = productClient.getVariants(List.of(variantId)).getData().get(0);
        return variant.getSku();
    }

    private BigDecimal getVariantPrice(CartItemResponse cartItem) {
        if (cartItem.getVariant() != null && cartItem.getVariant().getPriceOverride() != null) {
            return cartItem.getVariant().getPriceOverride();
        }
        if (cartItem.getVariant() != null && cartItem.getVariant().getProduct() != null) {
            return cartItem.getVariant().getProduct().getBasePrice();
        }
        return BigDecimal.ZERO;
    }

    public OrderResponse getOrder(String userId, String orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Không tìm thấy đơn hàng", 404));
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return buildOrderResponse(order, items);
    }

    public List<OrderResponse> getOrders(String userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream()
                .map(order -> buildOrderResponse(order, orderItemRepository.findByOrderId(order.getId())))
                .collect(Collectors.toList());
    }

    public OrderResponse updateOrderStatus(String orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Không tìm thấy đơn hàng", 404));

        order.setOrderStatus(request.getOrderStatus());
        order = orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return buildOrderResponse(order, items);
    }

    // Internal endpoint for payment service callback
    public void updateOrderStatusFromPayment(String orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Không tìm thấy đơn hàng", 404));

        order.setOrderStatus(status);
        orderRepository.save(order);
    }

    private OrderResponse buildOrderResponse(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .orderId(item.getOrderId())
                        .variantId(item.getVariantId())
                        .quantity(item.getQuantity())
                        .priceAtPurchase(item.getPriceAtPurchase())
                        .isAiConversion(item.getIsAiConversion())
                        .sourceBundleId(item.getSourceBundleId())
                        .createdAt(item.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus())
                .shippingAddress(order.getShippingAddress())
                .items(itemResponses)
                .createdAt(order.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .updatedAt(order.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .build();
    }

    private com.stylemind.common.dto.ApiResponse<CartResponse> getCart(String authHeader) {
        return cartClient.getCart(authHeader, null);
    }
}