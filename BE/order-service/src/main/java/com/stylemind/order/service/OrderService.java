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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final CheckoutIdempotencyRepository checkoutIdempotencyRepository;
    private final CartClient cartClient;
    private final PaymentClient paymentClient;
    private final ProductClient productClient;
    private final UserClient userClient;
    private final NotificationClient notificationClient;
    private final OrderStatusService orderStatusService;

    private static final String CHECKOUT_STATUS_PROCESSING = "PROCESSING";
    private static final String CHECKOUT_STATUS_SUCCEEDED = "SUCCEEDED";
    private static final String CHECKOUT_STATUS_FAILED = "FAILED";

    public OrderResponse createOrder(String userId, String authHeader, String idempotencyKey, CreateOrderRequest request) {
        CheckoutIdempotency checkoutIdempotency = acquireCheckoutIdempotency(userId, idempotencyKey);
        if (checkoutIdempotency != null && CHECKOUT_STATUS_SUCCEEDED.equals(checkoutIdempotency.getStatus())
                && StringUtils.hasText(checkoutIdempotency.getOrderId())) {
            return buildExistingOrderResponse(userId, checkoutIdempotency.getOrderId());
        }
        try {
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
            OrderStatus initialStatus = "sepay".equals(paymentMethod) ? OrderStatus.PAYMENT_PENDING : OrderStatus.PENDING;
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

            PaymentClient.PaymentResponse paymentResponse;
            try {
                paymentResponse = "sepay".equals(paymentMethod)
                        ? createSepayPaymentTransaction(orderId, userId, order.getTotalAmount())
                        : createCodPaymentTransaction(orderId, userId, order.getTotalAmount());
            } catch (Exception ex) {
                log.error("Payment initialization failed for order: {}", orderId, ex);
                orderStatusService.changeStatus(order, OrderStatus.CANCELLED, userId);
                throw new BusinessException("PAYMENT_INIT_FAILED", "Unable to initialize payment: " + ex.getMessage(), 400);
            }

            if ("cod".equals(paymentMethod)) {
                // COD has no payment gateway step - the order is confirmed immediately.
                // The transaction row created above just tracks the collect-on-delivery
                // amount; its own status stays PENDING until the courier collects it.
                order = orderStatusService.changeStatus(order, OrderStatus.CONFIRMED, userId);
                clearCartBestEffort(authHeader, orderId);
                notifyOrderBestEffort(order, "ORDER_CONFIRMED", "Order confirmed",
                        "Your order " + orderId + " has been confirmed and will be paid on delivery.");
            }
            // sepay: order stays PAYMENT_PENDING. There is no customer confirmation step -
            // payment-service's SePay webhook reconciles the bank transfer and calls back
            // into updateOrderStatusFromPayment() below, asynchronously.

            OrderResponse response = buildOrderResponse(order, orderItems);
            applyPaymentResponse(response, paymentResponse);
            markCheckoutAttemptSucceeded(checkoutIdempotency, orderId);
            return response;
        } catch (RuntimeException ex) {
            markCheckoutAttemptFailed(checkoutIdempotency, ex.getMessage());
            throw ex;
        }
    }

    private PaymentClient.PaymentResponse createCodPaymentTransaction(String orderId, String userId, BigDecimal amount) {
        PaymentClient.CodCheckoutRequest paymentRequest = PaymentClient.CodCheckoutRequest.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .build();
        return unwrapPaymentResponse(paymentClient.createCodPayment(paymentRequest));
    }

    private PaymentClient.PaymentResponse createSepayPaymentTransaction(String orderId, String userId, BigDecimal amount) {
        PaymentClient.SepayCheckoutRequest paymentRequest = PaymentClient.SepayCheckoutRequest.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .build();
        return unwrapPaymentResponse(paymentClient.createSepayPayment(paymentRequest));
    }

    private PaymentClient.PaymentResponse unwrapPaymentResponse(com.stylemind.common.dto.ApiResponse<PaymentClient.PaymentResponse> response) {
        if (response == null || !response.isSuccess() || response.getData() == null
                || response.getData().getTransactionId() == null) {
            throw new BusinessException("PAYMENT_INIT_FAILED", "Payment service did not create a transaction", 502);
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
        OrderResponse response = buildOrderResponse(order, items);
        applyPaymentStatusIfAvailable(orderId, response);
        return response;
    }

    public List<OrderResponse> getOrders(String userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream()
                .map(order -> {
                    OrderResponse response = buildOrderResponse(order, orderItemRepository.findByOrderId(order.getId()));
                    applyPaymentStatusIfAvailable(order.getId(), response);
                    return response;
                })
                .collect(Collectors.toList());
    }

    public org.springframework.data.domain.Page<OrderResponse> getAllOrdersForAdmin(
            String status, String userId, java.time.LocalDateTime fromDate, java.time.LocalDateTime toDate,
            org.springframework.data.domain.Pageable pageable) {
        OrderStatus statusFilter = null;
        if (org.springframework.util.StringUtils.hasText(status)) {
            try {
                statusFilter = OrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("INVALID_ORDER_STATUS_FILTER", "Unknown order status: " + status, 400);
            }
        }
        String userIdFilter = org.springframework.util.StringUtils.hasText(userId) ? userId : null;
        return orderRepository.search(statusFilter, userIdFilter, fromDate, toDate, pageable)
                .map(order -> buildOrderResponse(order, orderItemRepository.findByOrderId(order.getId())));
    }

    public OrderResponse getOrderForAdmin(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", 404));
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return buildOrderResponse(order, items);
    }

    /**
     * Real order/revenue aggregates for the admin dashboard. Revenue counts only
     * orders whose payment has been received and are progressing or done
     * (PAID/CONFIRMED/PROCESSING/SHIPPED/COMPLETED) — never PENDING/PAYMENT_PENDING
     * (unpaid) or CANCELLED/EXPIRED/FAILED.
     */
    @Transactional(readOnly = true)
    public AdminOrderSummaryResponse getAdminSummary() {
        var revenueStatuses = java.util.EnumSet.of(
                OrderStatus.PAID, OrderStatus.CONFIRMED, OrderStatus.PROCESSING,
                OrderStatus.SHIPPED, OrderStatus.COMPLETED);
        java.time.LocalDateTime startOfToday = java.time.LocalDate.now().atStartOfDay();

        return AdminOrderSummaryResponse.builder()
                .totalOrders(orderRepository.count())
                .pendingOrders(orderRepository.countByStatuses(
                        java.util.EnumSet.of(OrderStatus.PENDING, OrderStatus.PAYMENT_PENDING)))
                .paidOrders(orderRepository.countByStatuses(java.util.EnumSet.of(OrderStatus.PAID)))
                .completedOrders(orderRepository.countByStatuses(java.util.EnumSet.of(OrderStatus.COMPLETED)))
                .cancelledOrders(orderRepository.countByStatuses(
                        java.util.EnumSet.of(OrderStatus.CANCELLED, OrderStatus.EXPIRED, OrderStatus.FAILED)))
                .todayOrders(orderRepository.countCreatedSince(startOfToday))
                .totalRevenue(orderRepository.sumRevenueByStatuses(revenueStatuses))
                .todayRevenue(orderRepository.sumRevenueByStatusesSince(revenueStatuses, startOfToday))
                .build();
    }

    public OrderResponse updateOrderStatusForAdmin(String orderId, UpdateOrderStatusRequest request, String adminUserId) {
        OrderStatus target = OrderStatus.valueOf(request.getOrderStatus());
        Order order = orderStatusService.changeStatus(orderId, target, adminUserId);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return buildOrderResponse(order, items);
    }

    // Called by payment-service after it reconciles a SePay webhook (see
    // InternalOrderController). There is no customer confirmation step for SePay -
    // this is the only place a PAYMENT_PENDING order ever resolves.
    public void updateOrderStatusFromPayment(String orderId, String paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", 404));

        if (order.getOrderStatus() != OrderStatus.PAYMENT_PENDING) {
            // Idempotent no-op: a redelivered webhook, or the order already expired
            // via OrderTimeoutJob before this callback arrived.
            log.info("Ignoring payment-status callback for order {} - not PAYMENT_PENDING (current={})",
                    orderId, order.getOrderStatus());
            return;
        }

        if ("PAID".equalsIgnoreCase(paymentStatus)) {
            order = orderStatusService.changeStatus(order, OrderStatus.PAID, "PAYMENT_WEBHOOK");
            clearCartByUserIdBestEffort(order.getUserId(), orderId);
            notifyOrderBestEffort(order, "ORDER_PAID", "Payment received",
                    "Payment for your order " + orderId + " has been received.");
        } else if ("FAILED".equalsIgnoreCase(paymentStatus)) {
            orderStatusService.changeStatus(order, OrderStatus.FAILED, "PAYMENT_WEBHOOK");
        }
    }

    private CheckoutIdempotency acquireCheckoutIdempotency(String userId, String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return null;
        }

        return checkoutIdempotencyRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .map(existing -> {
                    if (CHECKOUT_STATUS_SUCCEEDED.equals(existing.getStatus())) {
                        return existing;
                    }
                    if (CHECKOUT_STATUS_PROCESSING.equals(existing.getStatus())) {
                        throw new BusinessException(
                                "CHECKOUT_IN_PROGRESS",
                                "Đơn hàng đang được xử lý, vui lòng đợi trong giây lát.",
                                409
                        );
                    }
                    throw new BusinessException(
                            "CHECKOUT_RETRY_REQUIRED",
                            "Yêu cầu thanh toán trước đó không thành công. Vui lòng thử lại.",
                            409
                    );
                })
                .orElseGet(() -> createCheckoutIdempotency(userId, idempotencyKey));
    }

    private CheckoutIdempotency createCheckoutIdempotency(String userId, String idempotencyKey) {
        CheckoutIdempotency entry = CheckoutIdempotency.builder()
                .id(StringUtil.generateUniqueId())
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .status(CHECKOUT_STATUS_PROCESSING)
                .build();
        try {
            return checkoutIdempotencyRepository.save(entry);
        } catch (DataIntegrityViolationException ex) {
            return acquireCheckoutIdempotency(userId, idempotencyKey);
        }
    }

    private void markCheckoutAttemptSucceeded(CheckoutIdempotency checkoutIdempotency, String orderId) {
        if (checkoutIdempotency == null) {
            return;
        }
        checkoutIdempotency.setOrderId(orderId);
        checkoutIdempotency.setStatus(CHECKOUT_STATUS_SUCCEEDED);
        checkoutIdempotency.setErrorMessage(null);
        checkoutIdempotencyRepository.save(checkoutIdempotency);
    }

    private void markCheckoutAttemptFailed(CheckoutIdempotency checkoutIdempotency, String errorMessage) {
        if (checkoutIdempotency == null) {
            return;
        }
        checkoutIdempotency.setStatus(CHECKOUT_STATUS_FAILED);
        checkoutIdempotency.setErrorMessage(errorMessage);
        checkoutIdempotencyRepository.save(checkoutIdempotency);
    }

    private OrderResponse buildExistingOrderResponse(String userId, String orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", 404));
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        OrderResponse response = buildOrderResponse(order, items);
        applyPaymentStatusIfAvailable(orderId, response);
        return response;
    }

    private void applyPaymentStatusIfAvailable(String orderId, OrderResponse response) {
        try {
            PaymentClient.PaymentResponse paymentResponse = unwrapPaymentResponse(paymentClient.getPaymentStatus(orderId));
            applyPaymentResponse(response, paymentResponse);
        } catch (Exception ex) {
            log.debug("No payment status available for order {}: {}", orderId, ex.getMessage());
        }
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
                .orderStatus(order.getOrderStatus().name())
                .availableTransitions(order.getOrderStatus().allowedTransitions().stream().map(Enum::name).collect(Collectors.toList()))
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
        orderResponse.setQrContent(paymentResponse.getQrContent());
        orderResponse.setQrImageUrl(paymentResponse.getQrImageUrl());
        orderResponse.setTransferContent(paymentResponse.getTransferContent());
        orderResponse.setPaymentExpiresAt(paymentResponse.getExpiresAt());
    }

    private void clearCartBestEffort(String authHeader, String orderId) {
        try {
            cartClient.clearCart(authHeader);
        } catch (Exception ex) {
            log.warn("Failed to clear cart after order {} - cart may still show purchased items", orderId, ex);
        }
    }

    // Used by the webhook-driven path, which has no end-user Authorization header
    // to forward (SePay calls payment-service, which calls us, server-to-server).
    private void clearCartByUserIdBestEffort(String userId, String orderId) {
        try {
            cartClient.clearCartByUserId(userId);
        } catch (Exception ex) {
            log.warn("Failed to clear cart for user {} after order {} - cart may still show purchased items",
                    userId, orderId, ex);
        }
    }

    // Compensation guardrail: a notification failure must never roll back an
    // already-confirmed/paid order. Swallows all exceptions after a few quick
    // retries and only logs - the caller's transaction (and returned response)
    // proceeds regardless of whether this succeeds.
    private static final int NOTIFY_MAX_ATTEMPTS = 3;

    private void notifyOrderBestEffort(Order order, String type, String title, String content) {
        for (int attempt = 1; attempt <= NOTIFY_MAX_ATTEMPTS; attempt++) {
            try {
                var userResponse = userClient.getUserEmail(order.getUserId());
                String email = userResponse != null && userResponse.getData() != null
                        ? userResponse.getData().getEmail() : null;
                if (email == null || email.isBlank()) {
                    log.warn("No email on file for user {} - skipping {} notification for order {}",
                            order.getUserId(), type, order.getId());
                    return;
                }

                notificationClient.sendEmail(NotificationClient.EmailRequest.builder()
                        .userId(order.getUserId())
                        .recipientEmail(email)
                        .type(type)
                        .title(title)
                        .content(content)
                        .build());
                return;
            } catch (Exception ex) {
                if (attempt == NOTIFY_MAX_ATTEMPTS) {
                    log.warn("Failed to send {} notification for order {} after {} attempts - order is not affected: {}",
                            type, order.getId(), attempt, ex.getMessage());
                } else {
                    log.debug("Notification attempt {} failed for order {}, retrying: {}", attempt, order.getId(), ex.getMessage());
                }
            }
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
