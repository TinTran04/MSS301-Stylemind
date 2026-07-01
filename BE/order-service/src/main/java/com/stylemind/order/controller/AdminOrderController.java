package com.stylemind.order.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.order.dto.OrderResponse;
import com.stylemind.order.dto.UpdateOrderStatusRequest;
import com.stylemind.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders() {
        List<OrderResponse> orders = orderService.getAllOrdersForAdmin();
        return ResponseEntity.ok(ApiResponse.success("Admin orders fetched successfully", orders));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable String orderId) {
        OrderResponse order = orderService.getOrderForAdmin(orderId);
        return ResponseEntity.ok(ApiResponse.success("Admin order fetched successfully", order));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse order = orderService.updateOrderStatusForAdmin(orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Admin order status updated successfully", order));
    }
}
