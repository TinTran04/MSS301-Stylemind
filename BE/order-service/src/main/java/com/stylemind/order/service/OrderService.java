package com.stylemind.order.service;

import com.stylemind.cart.dto.CartItemResponse;
import com.stylemind.cart.dto.CartResponse;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import com.stylemind.order.dto.*;
import com.stylemind.order.entity.*;
import com.stylemind.order.feign.*;
import com.stylemind.order.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        CartResponse cart = getCart(authHeader).getData();
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessException("CART_EMPTY", "Cart is empty", 400);
        }

        List<OrderItemDraft> itemDrafts = cart.getItems().stream()
                .map(this::buildOrderItemDraft)
                .collect(Collectors.toList());

        BigDecimal totalAmount = itemDrafts.stream()
                .map(OrderItemDraft::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String paymentMethod = request.getPaymentMethod();
        String initialStatus = "online_simulated".equals(paymentMethod) ? "PENDING_PAYMENT" : "PENDING";
        String orderId = StringUtil.generateUniqueId();

        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .totalAmount(totalAmount)
                .orderStatus(initialStatus)
                .shippingAddress(request.getShippingAddress())
                .build();

        order = orderRepository.save(order);

        List<OrderItem> orderItems = itemDrafts.stream().map(draft -> {
            OrderItem item = OrderItem.builder()
                    .id(StringUtil.generateUniqueId())
                    .orderId(orderId)
                    .variantId(draft.variantId())
                    .quantity(draft.quantity())
                    .priceAtPurchase(draft.unitPrice())
                    .isAiConversion(draft.isAiConversion())
                    .sourceBundleId(draft.sourceBundleId())
                    .build();
            return orderItemRepository.save(item);
        }).collect(Collectors.toList());

        PaymentClient.PaymentResponse paymentResponse = null;
        if ("online_simulated".equals(paymentMethod)) {
            try {
                paymentResponse = createPaymentTransaction(orderId, paymentMethod, order.getTotalAmount());
            } catch (Exception ex) {
                log.error("Payment initialization failed for order: {}", orderId, ex);
                order.setOrderStatus("CANCELLED");
                orderRepository.save(order);
                throw new BusinessException("PAYMENT_INIT_FAILED", "Unable to initialize payment: " + ex.getMessage(), 400);
            }
        }

        if ("cod".equals(paymentMethod)) {
            clearCartBestEffort(authHeader, orderId);
        }

        OrderResponse response = buildOrderResponse(order, orderItems);
        applyPaymentResponse(response, paymentResponse);
        return response;
    }

    public OrderResponse confirmOnlinePayment(
            String userId,
            String authHeader,
            String orderId,
            ConfirmPaymentRequest request
    ) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", 404));

        if (!"PENDING_PAYMENT".equals(order.getOrderStatus())) {
            throw new BusinessException("INVALID_ORDER_STATUS", "Order is not waiting for payment", 400);
        }

        PaymentClient.PaymentResponse paymentResponse = processPayment(
                orderId,
                request.getTransactionId(),
                request.getVerificationCode(),
                order.getTotalAmount()
        );

        String paymentStatus = paymentResponse.getStatus();
        if ("COMPLETED".equalsIgnoreCase(paymentStatus)) {
            order.setOrderStatus("PROCESSING");
            order = orderRepository.save(order);
            clearCartBestEffort(authHeader, orderId);
        } else if ("FAILED".equalsIgnoreCase(paymentStatus)) {
            order.setOrderStatus("CANCELLED");
            order = orderRepository.save(order);
        } else {
            throw new BusinessException("PAYMENT_FAILED", "Payment did not complete", 400);
        }

        OrderResponse response = buildOrderResponse(order, orderItemRepository.findByOrderId(orderId));
        applyPaymentResponse(response, paymentResponse);
        return response;
    }

    private PaymentClient.PaymentResponse createPaymentTransaction(String orderId, String method, BigDecimal amount) {
        PaymentClient.CheckoutRequest paymentRequest = PaymentClient.CheckoutRequest.builder()
                .orderId(orderId)
                .method(method)
                .amount(amount)
                .build();

        var response = paymentClient.checkout(paymentRequest);
        if (response == null || !response.isSuccess() || response.getData() == null
                || response.getData().getTransactionId() == null) {
            throw new BusinessException("PAYMENT_INIT_FAILED", "Payment service did not create a transaction", 502);
        }
        return response.getData();
    }

    private PaymentClient.PaymentResponse processPayment(
            String orderId,
            String transactionId,
            String verificationCode,
            BigDecimal amount
    ) {
        PaymentClient.ProcessPaymentRequest paymentRequest = PaymentClient.ProcessPaymentRequest.builder()
                .transactionId(transactionId)
                .orderId(orderId)
                .amount(amount)
                .verificationCode(verificationCode)
                .build();

        var response = paymentClient.processPayment(paymentRequest);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new BusinessException("PAYMENT_FAILED", "Payment service returned an empty response", 502);
        }
        return response.getData();
    }

    private OrderItemDraft buildOrderItemDraft(CartItemResponse cartItem) {
        String variantId = cartItem.getVariantId();
        if (variantId == null || variantId.isBlank()) {
            throw new BusinessException("INVALID_CART_ITEM", "Cart item is missing variantId", 400);
        }

        Integer quantity = cartItem.getQuantity();
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("INVALID_CART_ITEM", "Cart item quantity is invalid", 400);
        }

        ProductClient.VariantSnapshot snapshot = getVariantSnapshot(variantId);
        BigDecimal unitPrice = snapshot.getEffectivePrice();
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    "VARIANT_PRICE_UNAVAILABLE",
                    "Valid price is unavailable for variant: " + variantId,
                    400
            );
        }

        if (!"ACTIVE".equalsIgnoreCase(snapshot.getStatus())) {
            throw new BusinessException(
                    "PRODUCT_NOT_ACTIVE",
                    "Product variant is not active: " + variantId,
                    400
            );
        }

        return new OrderItemDraft(
                variantId,
                quantity,
                unitPrice,
                Boolean.TRUE.equals(cartItem.getIsAiRecommended()),
                cartItem.getSourceBundleId()
        );
    }

    private ProductClient.VariantSnapshot getVariantSnapshot(String variantId) {
        try {
            var response = productClient.getVariantSnapshot(variantId);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new BusinessException(
                        "VARIANT_NOT_FOUND",
                        "Variant not found: " + variantId,
                        404
                );
            }
            return response.getData();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to fetch variant snapshot for {}: {}", variantId, ex.getMessage());
            throw new BusinessException(
                    "VARIANT_PRICE_UNAVAILABLE",
                    "Unable to fetch product price for variant: " + variantId,
                    502
            );
        }
    }

    public OrderResponse getOrder(String userId, String orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", 404));
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return buildOrderResponse(order, items);
    }

    public List<OrderResponse> getOrders(String userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream()
                .map(order -> buildOrderResponse(order, orderItemRepository.findByOrderId(order.getId())))
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getAllOrdersForAdmin() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(order -> buildOrderResponse(order, orderItemRepository.findByOrderId(order.getId())))
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderForAdmin(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", 404));
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return buildOrderResponse(order, items);
    }

    public OrderResponse updateOrderStatusForAdmin(String orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", 404));

        order.setOrderStatus(request.getOrderStatus());
        order = orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return buildOrderResponse(order, items);
    }

    public void updateOrderStatusFromPayment(String orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", 404));

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

    private void applyPaymentResponse(OrderResponse orderResponse, PaymentClient.PaymentResponse paymentResponse) {
        if (paymentResponse == null) {
            return;
        }
        orderResponse.setPaymentTransactionId(paymentResponse.getTransactionId());
        orderResponse.setPaymentStatus(paymentResponse.getStatus());
    }

    private void clearCartBestEffort(String authHeader, String orderId) {
        try {
            cartClient.clearCart(authHeader);
        } catch (Exception ex) {
            log.warn("Failed to clear cart after order {} - cart may still show purchased items", orderId, ex);
        }
    }

    private com.stylemind.common.dto.ApiResponse<CartResponse> getCart(String authHeader) {
        return cartClient.getCart(authHeader, null);
    }

    private record OrderItemDraft(
            String variantId,
            Integer quantity,
            BigDecimal unitPrice,
            Boolean isAiConversion,
            String sourceBundleId
    ) {
        BigDecimal lineTotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
