package com.stylemind.order.service;

import com.stylemind.common.dto.PageResponse;
import com.stylemind.order.dto.AdminOrderSummaryResponse;
import com.stylemind.order.dto.AdminOrdersResponse;
import com.stylemind.order.dto.CreateOrderRequest;
import com.stylemind.order.dto.OrderResponse;
import com.stylemind.order.dto.OrderSummaryResponse;
import com.stylemind.order.dto.UpdateOrderStatusRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

public interface OrderService {

    OrderResponse createOrder(String userId, String authHeader, String idempotencyKey, CreateOrderRequest request);

    OrderResponse getOrder(String userId, String orderId);

    OrderResponse cancelOrder(String userId, String orderId);

    PageResponse<OrderSummaryResponse> getOrdersPage(String userId, Pageable pageable);

    PageResponse<OrderSummaryResponse> getOrdersPage(String userId, String status, Pageable pageable);

    AdminOrdersResponse getAllOrdersForAdmin(
            String status,
            String cancellationStatus,
            String userId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable);

    OrderResponse getOrderForAdmin(String orderId);

    OrderResponse uploadDeliveryImage(String userId, String orderId, MultipartFile file);

    AdminOrderSummaryResponse getAdminSummary();

    OrderResponse updateOrderStatusForAdmin(String orderId, UpdateOrderStatusRequest request, String adminUserId);

    void updateOrderStatusFromPayment(String orderId, String paymentStatus);
}
