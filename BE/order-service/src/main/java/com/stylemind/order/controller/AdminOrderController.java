package com.stylemind.order.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.security.UserPrincipal;
import com.stylemind.order.dto.AdminOrdersResponse;
import com.stylemind.order.dto.AdminOrderSummaryResponse;
import com.stylemind.order.dto.OrderResponse;
import com.stylemind.order.dto.UpdateOrderStatusRequest;
import com.stylemind.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminOrdersResponse>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        AdminOrdersResponse result = orderService.getAllOrdersForAdmin(
                status,
                userId,
                fromDate != null ? fromDate.atStartOfDay() : null,
                toDate != null ? toDate.plusDays(1).atStartOfDay() : null,
                pageable);
        return ResponseEntity.ok(ApiResponse.success("Admin orders fetched successfully", result));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminOrderSummaryResponse>> getSummary() {
        AdminOrderSummaryResponse summary = orderService.getAdminSummary();
        return ResponseEntity.ok(ApiResponse.success("Admin order summary fetched successfully", summary));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable String orderId) {
        OrderResponse order = orderService.getOrderForAdmin(orderId);
        return ResponseEntity.ok(ApiResponse.success("Admin order fetched successfully", order));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse order = orderService.updateOrderStatusForAdmin(orderId, request, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Admin order status updated successfully", order));
    }
}
