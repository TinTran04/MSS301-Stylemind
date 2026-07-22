package com.stylemind.order.service.impl;

import com.stylemind.cart.dto.CartItemResponse;
import com.stylemind.cart.dto.CartResponse;
import com.stylemind.common.dto.PageResponse;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import com.stylemind.order.dto.*;
import com.stylemind.order.entity.*;
import com.stylemind.order.feign.*;
import com.stylemind.order.repository.*;
import com.stylemind.order.service.AdminRevenueService;
import com.stylemind.order.service.OrderService;
import com.stylemind.order.service.OrderStatusService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CheckoutIdempotencyRepository checkoutIdempotencyRepository;
    private final CartClient cartClient;
    private final PaymentClient paymentClient;
    private final ProductClient productClient;
    private final UserClient userClient;
    private final com.stylemind.order.feign.UserAddressClient userAddressClient;
    private final OrderStatusAuditLogRepository auditLogRepository;
    private final OrderStatusService orderStatusService;
    private final OrderDeliveryImageRepository deliveryImageRepository;
    private final AdminRevenueService adminRevenueService;

    @Value("${app.reporting.timezone:Asia/Ho_Chi_Minh}")
    private String reportingTimezone;

    @Value("${app.reporting.database-timezone:UTC}")
    private String reportingDatabaseTimezone;

    private static final String CHECKOUT_STATUS_PROCESSING = "PROCESSING";
    private static final String CHECKOUT_STATUS_SUCCEEDED = "SUCCEEDED";
    private static final String CHECKOUT_STATUS_FAILED = "FAILED";
    private static final BigDecimal VAT_RATE = new BigDecimal("0.10");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("200000");
    private static final BigDecimal STANDARD_SHIPPING_FEE = new BigDecimal("15000");
    private static final long MAX_DELIVERY_IMAGE_BYTES = 3L * 1024 * 1024;
    private static final int MAX_DELIVERY_IMAGES_PER_ORDER = 5;
    @Override
    public OrderResponse createOrder(String userId, String authHeader, String idempotencyKey, CreateOrderRequest request) {
        CheckoutIdempotency checkoutIdempotency = acquireCheckoutIdempotency(userId, idempotencyKey);
        if (checkoutIdempotency != null && CHECKOUT_STATUS_SUCCEEDED.equals(checkoutIdempotency.getStatus())
                && StringUtils.hasText(checkoutIdempotency.getOrderId())) {
            return buildExistingOrderResponse(userId, checkoutIdempotency.getOrderId());
        }
        try {
            UserAddressClient.DeliveryAddressSnapshot address = getCheckoutAddress(userId, request.getAddressId());
            CartResponse cart = getCart(authHeader).getData();
            if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
                throw new BusinessException("CART_EMPTY", "Cart is empty", 400);
            }

            List<OrderItemDraft> itemDrafts = cart.getItems().stream()
                    .map(this::buildOrderItemDraft)
                    .collect(Collectors.toList());

            String paymentMethod = request.getPaymentMethod();
            OrderPricing pricing = calculateOrderPricing(itemDrafts);
            OrderStatus initialStatus = "sepay".equals(paymentMethod) ? OrderStatus.PAYMENT_PENDING : OrderStatus.PENDING;
            String orderId = StringUtil.generateUniqueId();

            Order order = Order.builder()
                    .id(orderId)
                    .userId(userId)
                    .subtotalAmount(pricing.subtotalAmount())
                    .shippingFee(pricing.shippingFee())
                    .taxAmount(pricing.taxAmount())
                    .roundingAdjustment(pricing.roundingAdjustment())
                    .totalAmount(pricing.totalAmount())
                    .orderStatus(initialStatus)
                    .shippingAddress(formatShippingAddress(address))
                    .sourceAddressId(address.getId())
                    .shippingRecipientName(address.getRecipientName())
                    .shippingPhone(address.getPhoneNumber())
                    .shippingProvinceCode(address.getProvinceCode())
                    .shippingProvinceName(address.getProvinceName())
                    .shippingWardCode(address.getWardCode())
                    .shippingWardName(address.getWardName())
                    .shippingAddressLine(address.getAddressLine())
                    .shippingNote(address.getShippingNote())
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

    private OrderPricing calculateOrderPricing(List<OrderItemDraft> itemDrafts) {
        BigDecimal subtotal = itemDrafts.stream()
                .map(OrderItemDraft::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal shippingFee = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : STANDARD_SHIPPING_FEE.setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = subtotal.multiply(VAT_RATE).setScale(0, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(shippingFee).add(taxAmount).setScale(2, RoundingMode.HALF_UP);
        return new OrderPricing(subtotal, shippingFee, taxAmount, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), totalAmount);
    }

    private UserAddressClient.DeliveryAddressSnapshot getCheckoutAddress(String userId, String addressId) {
        if (!StringUtils.hasText(addressId)) {
            throw new BusinessException("SHIPPING_ADDRESS_REQUIRED", "Vui lòng chọn địa chỉ giao hàng", 400);
        }
        try {
            var response = userAddressClient.getAddress(userId, addressId);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new BusinessException("SHIPPING_ADDRESS_NOT_FOUND", "Không tìm thấy địa chỉ giao hàng", 404);
            }
            UserAddressClient.DeliveryAddressSnapshot address = response.getData();
            if (!userId.equals(address.getUserId()) || !"VALID".equals(address.getValidationStatus())) {
                throw new BusinessException("SHIPPING_ADDRESS_NOT_VALIDATED", "Địa chỉ giao hàng chưa được xác thực", 409);
            }
            return address;
        } catch (BusinessException ex) {
            throw ex;
        } catch (FeignException ex) {
            if (ex.status() == 404) {
                throw new BusinessException("SHIPPING_ADDRESS_NOT_FOUND", "Không tìm thấy địa chỉ giao hàng", 404);
            }
            if (ex.status() == 403) {
                throw new BusinessException("SHIPPING_ADDRESS_NOT_OWNED", "Địa chỉ giao hàng không thuộc tài khoản này", 403);
            }
            if (ex.status() == 409) {
                throw new BusinessException("SHIPPING_ADDRESS_NOT_VALIDATED", "Địa chỉ giao hàng chưa được xác thực", 409);
            }
            log.warn("Address validation service failed for user {} with status {}", userId, ex.status());
            throw new BusinessException("SHIPPING_ADDRESS_UNAVAILABLE", "Không thể xác thực địa chỉ giao hàng", 502);
        } catch (Exception ex) {
            log.warn("Unable to validate checkout address for user {}: {}", userId, ex.getMessage());
            throw new BusinessException("SHIPPING_ADDRESS_UNAVAILABLE", "Không thể xác thực địa chỉ giao hàng", 502);
        }
    }

    private String formatShippingAddress(UserAddressClient.DeliveryAddressSnapshot address) {
        return java.util.stream.Stream.of(address.getAddressLine(), address.getWardName(), address.getProvinceName())
                .filter(org.springframework.util.StringUtils::hasText)
                .collect(Collectors.joining(", "));
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

    @Override
    public OrderResponse getOrder(String userId, String orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", 404));
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        OrderResponse response = buildOrderResponse(order, items);
        applyPaymentStatusIfAvailable(orderId, response);
        enrichOrderItems(response);
        enrichStatusHistory(orderId, response);
        enrichDeliveryImages(orderId, response);
        return response;
    }

    @Override
    public OrderResponse cancelOrder(String userId, String orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", 404));

        OrderStatus currentStatus = order.getOrderStatus();
        if (currentStatus != OrderStatus.PENDING && currentStatus != OrderStatus.PAYMENT_PENDING) {
            throw new BusinessException(
                    "ORDER_CANCEL_NOT_ALLOWED",
                    "Không thể hủy thanh toán cho đơn hàng này.",
                    409
            );
        }

        if (currentStatus == OrderStatus.PAYMENT_PENDING) {
            expirePaymentBeforeCancellation(orderId);
        }

        // Keep the local order transition after the payment-side compensation
        // succeeds, so a failed internal call cannot leave CANCELLED + PENDING.
        Order cancelled = orderStatusService.changeStatus(order, OrderStatus.CANCELLED, userId);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        OrderResponse response = buildOrderResponse(cancelled, items);
        applyPaymentStatusIfAvailable(orderId, response);
        return response;
    }

    private void expirePaymentBeforeCancellation(String orderId) {
        try {
            com.stylemind.common.dto.ApiResponse<Void> response = paymentClient.expirePaymentByOrderId(orderId);
            if (response == null || !response.isSuccess()) {
                throw new BusinessException(
                        "PAYMENT_CANCEL_FAILED",
                        "Không thể hủy thanh toán cho đơn hàng này.",
                        502
                );
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to expire payment before cancelling order {}: {}", orderId, ex.getMessage());
            throw new BusinessException(
                    "PAYMENT_CANCEL_FAILED",
                    "Không thể hủy thanh toán cho đơn hàng này.",
                    502
            );
        }
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<OrderSummaryResponse> getOrdersPage(String userId, Pageable pageable) {
        return getOrdersPage(userId, null, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<OrderSummaryResponse> getOrdersPage(String userId, String status, Pageable pageable) {
        OrderStatus statusFilter = null;
        if (StringUtils.hasText(status)) {
            try {
                statusFilter = OrderStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("INVALID_ORDER_STATUS_FILTER", "Unknown order status: " + status, 400);
            }
        }

        Page<Order> orders = statusFilter == null
                ? orderRepository.findByUserId(userId, pageable)
                : orderRepository.findByUserIdAndOrderStatus(userId, statusFilter, pageable);
        List<String> orderIds = orders.getContent().stream()
                .map(Order::getId)
                .toList();
        Map<String, Long> itemCounts = orderIds.isEmpty()
                ? Map.of()
                : orderItemRepository.countByOrderIds(orderIds).stream()
                        .collect(Collectors.toMap(
                                OrderItemCountResponse::orderId,
                                OrderItemCountResponse::itemCount));

        return PageResponse.of(orders.map(order -> OrderSummaryResponse.builder()
                .id(order.getId())
                .createdAt(order.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .orderStatus(order.getOrderStatus().name())
                .totalAmount(order.getTotalAmount())
                .itemCount(itemCounts.getOrDefault(order.getId(), 0L).intValue())
                .build()));
    }

    @Override
    public com.stylemind.order.dto.AdminOrdersResponse getAllOrdersForAdmin(
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

        var page = orderRepository.search(statusFilter, userIdFilter, fromDate, toDate, pageable)
                .map(order -> buildOrderResponse(order, orderItemRepository.findByOrderId(order.getId())));

        var revenue = adminRevenueService.calculate(fromDate, toDate, statusFilter, userIdFilter);

        return com.stylemind.order.dto.AdminOrdersResponse.builder()
                .page(page)
                .totalRevenue(revenue.getNetRevenue())
                .netRevenue(revenue.getNetRevenue())
                .vatCollected(revenue.getVatCollected())
                .shippingFeesCollected(revenue.getShippingFeesCollected())
                .grossCustomerPayments(revenue.getGrossCustomerPayments())
                .refundAmount(revenue.getRefundAmount())
                .recognizedOrderCount(revenue.getRecognizedOrderCount())
                .sepayRecognizedRevenue(revenue.getSepayRecognizedRevenue())
                .codRecognizedRevenue(revenue.getCodRecognizedRevenue())
                .currency("VND")
                .build();
    }

    @Override
    public OrderResponse getOrderForAdmin(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", 404));
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        OrderResponse response = buildOrderResponse(order, items);
        applyPaymentStatusIfAvailable(orderId, response);
        enrichAdminOrderDetails(response);
        enrichStatusHistory(orderId, response);
        enrichDeliveryImages(orderId, response);
        return response;
    }

    @Override
    public OrderResponse uploadDeliveryImage(String userId, String orderId, MultipartFile file) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", 404));
        validateDeliveryImageUpload(order, file);

        try {
            OrderDeliveryImage image = OrderDeliveryImage.builder()
                    .id(StringUtil.generateUniqueId())
                    .orderId(orderId)
                    .userId(userId)
                    .fileName(safeFileName(file.getOriginalFilename()))
                    .contentType(normalizeImageContentType(file.getContentType()))
                    .sizeBytes(file.getSize())
                    .imageData(file.getBytes())
                    .build();
            deliveryImageRepository.save(image);
        } catch (java.io.IOException ex) {
            throw new BusinessException("ORDER_DELIVERY_IMAGE_READ_FAILED", "Không thể đọc tệp ảnh nhận hàng.", 400);
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        OrderResponse response = buildOrderResponse(order, items);
        applyPaymentStatusIfAvailable(orderId, response);
        enrichOrderItems(response);
        enrichStatusHistory(orderId, response);
        enrichDeliveryImages(orderId, response);
        return response;
    }

    private void validateDeliveryImageUpload(Order order, MultipartFile file) {
        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException(
                    "ORDER_NOT_DELIVERED",
                    "Chỉ có thể tải ảnh nhận hàng sau khi đơn đã giao thành công.",
                    409
            );
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("ORDER_DELIVERY_IMAGE_EMPTY", "Vui lòng chọn ảnh nhận hàng.", 400);
        }
        if (file.getSize() > MAX_DELIVERY_IMAGE_BYTES) {
            throw new BusinessException(
                    "ORDER_DELIVERY_IMAGE_TOO_LARGE",
                    "Ảnh nhận hàng không được vượt quá 3MB.",
                    400
            );
        }
        String contentType = normalizeImageContentType(file.getContentType());
        if (!List.of("image/jpeg", "image/png", "image/webp").contains(contentType)) {
            throw new BusinessException(
                    "ORDER_DELIVERY_IMAGE_INVALID_TYPE",
                    "Chỉ hỗ trợ ảnh JPG, PNG hoặc WEBP.",
                    400
            );
        }
        if (deliveryImageRepository.countByOrderId(order.getId()) >= MAX_DELIVERY_IMAGES_PER_ORDER) {
            throw new BusinessException(
                    "ORDER_DELIVERY_IMAGE_LIMIT_REACHED",
                    "Mỗi đơn hàng chỉ được tải tối đa 5 ảnh nhận hàng.",
                    409
            );
        }
    }

    private String normalizeImageContentType(String contentType) {
        String value = StringUtils.hasText(contentType) ? contentType.trim().toLowerCase(Locale.ROOT) : "";
        if ("image/jpg".equals(value)) return "image/jpeg";
        return value;
    }

    private String safeFileName(String fileName) {
        String value = StringUtils.hasText(fileName) ? fileName.trim() : "delivery-image";
        return value.replaceAll("[\\\\/\\r\\n\\t]+", "_");
    }

    @Transactional(readOnly = true)
    @Override
    public AdminOrderSummaryResponse getAdminSummary() {
        ZoneId zone = ZoneId.of(reportingTimezone);
        ZoneId databaseZone = ZoneId.of(reportingDatabaseTimezone);
        LocalDate today = LocalDate.now(zone);
        LocalDateTime startOfToday = reportingBoundary(today, zone, databaseZone);
        LocalDateTime startOfTomorrow = reportingBoundary(today.plusDays(1), zone, databaseZone);
        AdminRevenueService.RevenueSummary allTime = adminRevenueService.calculate(null, null, null, null);
        AdminRevenueService.RevenueSummary todayRevenue = adminRevenueService.calculate(
                startOfToday, startOfTomorrow, null, null);

        return AdminOrderSummaryResponse.builder()
                .totalOrders(orderRepository.count())
                .pendingOrders(orderRepository.countByStatuses(
                        java.util.EnumSet.of(OrderStatus.PENDING, OrderStatus.PAYMENT_PENDING)))
                .paidOrders(orderRepository.countByStatuses(java.util.EnumSet.of(OrderStatus.PAID)))
                .completedOrders(orderRepository.countByStatuses(java.util.EnumSet.of(OrderStatus.COMPLETED)))
                .cancelledOrders(orderRepository.countByStatuses(
                        java.util.EnumSet.of(OrderStatus.CANCELLED, OrderStatus.EXPIRED, OrderStatus.FAILED)))
                .todayOrders(orderRepository.countCreatedSince(startOfToday))
                .totalRevenue(allTime.getNetRevenue())
                .netRevenue(allTime.getNetRevenue())
                .vatCollected(allTime.getVatCollected())
                .shippingFeesCollected(allTime.getShippingFeesCollected())
                .grossCustomerPayments(allTime.getGrossCustomerPayments())
                .refundAmount(allTime.getRefundAmount())
                .recognizedOrderCount(allTime.getRecognizedOrderCount())
                .sepayRecognizedRevenue(allTime.getSepayRecognizedRevenue())
                .codRecognizedRevenue(allTime.getCodRecognizedRevenue())
                .currency("VND")
                .todayNetRevenue(todayRevenue.getNetRevenue())
                .todayVatCollected(todayRevenue.getVatCollected())
                .todayShippingFeesCollected(todayRevenue.getShippingFeesCollected())
                .todayGrossCustomerPayments(todayRevenue.getGrossCustomerPayments())
                .todayRefundAmount(todayRevenue.getRefundAmount())
                .todayRecognizedOrderCount(todayRevenue.getRecognizedOrderCount())
                .todayRevenue(todayRevenue.getNetRevenue())
                .build();
    }

    private LocalDateTime reportingBoundary(LocalDate date, ZoneId reportingZone, ZoneId databaseZone) {
        return date.atStartOfDay(reportingZone)
                .withZoneSameInstant(databaseZone)
                .toLocalDateTime();
    }

    @Override
    public OrderResponse updateOrderStatusForAdmin(String orderId, UpdateOrderStatusRequest request, String adminUserId) {
        OrderStatus target = OrderStatus.valueOf(request.getOrderStatus());
        Order order = orderStatusService.changeStatus(orderId, target, adminUserId);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        OrderResponse response = buildOrderResponse(order, items);
        enrichStatusHistory(orderId, response);
        return response;
    }

    // Called by payment-service after it reconciles a SePay webhook (see
    // InternalOrderController). There is no customer confirmation step for SePay -
    // this is the only place a PAYMENT_PENDING order ever resolves.
    @Override
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
                .subtotalAmount(order.getSubtotalAmount())
                .shippingFee(order.getShippingFee())
                .taxAmount(order.getTaxAmount())
                .roundingAdjustment(order.getRoundingAdjustment())
                .orderStatus(order.getOrderStatus().name())
                .availableTransitions(order.getOrderStatus().allowedTransitions().stream().map(Enum::name).collect(Collectors.toList()))
                .shippingAddress(order.getShippingAddress())
                .sourceAddressId(order.getSourceAddressId())
                .shippingRecipientName(order.getShippingRecipientName())
                .shippingPhone(order.getShippingPhone())
                .shippingProvinceCode(order.getShippingProvinceCode())
                .shippingProvinceName(order.getShippingProvinceName())
                .shippingWardCode(order.getShippingWardCode())
                .shippingWardName(order.getShippingWardName())
                .shippingAddressLine(order.getShippingAddressLine())
                .shippingNote(order.getShippingNote())
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
        orderResponse.setPaymentMethod(paymentResponse.getMethod());
        orderResponse.setPaymentReference(paymentResponse.getTransactionRef());
        orderResponse.setGatewayTransactionId(paymentResponse.getGatewayTransactionId());
        orderResponse.setPaidAt(paymentResponse.getPaidAt());
    }

    private void enrichOrderItems(OrderResponse response) {
        if (response == null || response.getItems() == null) return;
        response.getItems().forEach(item -> {
            try {
                var variantResponse = productClient.getVariantSnapshot(item.getVariantId());
                if (variantResponse == null || !variantResponse.isSuccess() || variantResponse.getData() == null) {
                    return;
                }
                ProductClient.VariantSnapshot variant = variantResponse.getData();
                item.setCatalogVariantId(variant.getVariantId());
                item.setProductId(variant.getProductId());
                item.setProductName(variant.getProductName());
                item.setSku(variant.getSku());
                item.setSize(variant.getSize());
                item.setColor(variant.getColor());
                item.setMaterial(variant.getMaterial());
                item.setPrimaryImageUrl(variant.getPrimaryImageUrl());
            } catch (Exception ex) {
                log.debug("Variant enrichment unavailable for order item {}: {}", item.getId(), ex.getMessage());
            }
        });
    }

    private void enrichAdminOrderDetails(OrderResponse response) {
        try {
            var userResponse = userClient.getUserEmail(response.getUserId());
            if (userResponse != null && userResponse.isSuccess() && userResponse.getData() != null) {
                response.setCustomerEmail(userResponse.getData().getEmail());
            }
        } catch (Exception ex) {
            log.debug("Customer enrichment unavailable for admin order {}: {}", response.getId(), ex.getMessage());
        }

        enrichOrderItems(response);
    }

    private void enrichStatusHistory(String orderId, OrderResponse response) {
        response.setStatusHistory(auditLogRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(audit -> OrderStatusHistoryResponse.builder()
                        .id(audit.getId())
                        .previousStatus(audit.getFromStatus() != null ? audit.getFromStatus().name() : null)
                        .newStatus(audit.getToStatus().name())
                        .actor(audit.getActorId())
                        .timestamp(audit.getCreatedAt() != null
                                ? audit.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
                                : null)
                        .build())
                .collect(Collectors.toList()));
    }

    private void enrichDeliveryImages(String orderId, OrderResponse response) {
        response.setDeliveryImages(deliveryImageRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(this::mapToDeliveryImageResponse)
                .collect(Collectors.toList()));
    }

    private OrderDeliveryImageResponse mapToDeliveryImageResponse(OrderDeliveryImage image) {
        String contentType = normalizeImageContentType(image.getContentType());
        String imageDataUrl = "data:" + contentType + ";base64,"
                + Base64.getEncoder().encodeToString(image.getImageData());
        return OrderDeliveryImageResponse.builder()
                .id(image.getId())
                .orderId(image.getOrderId())
                .fileName(image.getFileName())
                .contentType(contentType)
                .sizeBytes(image.getSizeBytes())
                .imageDataUrl(imageDataUrl)
                .uploadedAt(image.getCreatedAt() != null
                        ? image.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
                        : null)
                .build();
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

    private record OrderPricing(
            BigDecimal subtotalAmount,
            BigDecimal shippingFee,
            BigDecimal taxAmount,
            BigDecimal roundingAdjustment,
            BigDecimal totalAmount
    ) {}
}
