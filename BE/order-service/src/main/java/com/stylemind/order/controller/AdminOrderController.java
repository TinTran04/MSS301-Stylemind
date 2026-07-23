package com.stylemind.order.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.security.UserPrincipal;
import com.stylemind.order.dto.AdminCancelOrderRequest;
import com.stylemind.order.dto.AdminOrdersResponse;
import com.stylemind.order.dto.AdminOrderSummaryResponse;
import com.stylemind.order.dto.CompleteOrderRefundRequest;
import com.stylemind.order.dto.FailOrderRefundRequest;
import com.stylemind.order.dto.OrderCancellationResponse;
import com.stylemind.order.dto.OrderCancellationSummaryResponse;
import com.stylemind.order.dto.OrderResponse;
import com.stylemind.order.dto.RejectOrderCancellationRequest;
import com.stylemind.order.dto.UpdateOrderStatusRequest;
import com.stylemind.order.dto.RefundSummaryResponse;
import com.stylemind.order.service.OrderCancellationService;
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
    private final OrderCancellationService orderCancellationService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminOrdersResponse>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String cancellationStatus,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        AdminOrdersResponse result = orderService.getAllOrdersForAdmin(
                status,
                cancellationStatus,
                userId,
                fromDate != null ? fromDate.atStartOfDay() : null,
                toDate != null ? toDate.atTime(23, 59, 59) : null,
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

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderCancellationResponse>> cancelOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String orderId,
            @Valid @RequestBody AdminCancelOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Admin cancellation created successfully",
                orderCancellationService.adminCancelOrder(principal.getUserId(), orderId, request)));
    }

    @PatchMapping("/order-cancellations/{id}/approve")
    public ResponseEntity<ApiResponse<OrderCancellationResponse>> approveCancellation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cancellation approved successfully",
                orderCancellationService.approveCancellation(principal.getUserId(), id)));
    }

    @PatchMapping("/order-cancellations/{id}/reject")
    public ResponseEntity<ApiResponse<OrderCancellationResponse>> rejectCancellation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody RejectOrderCancellationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cancellation rejected successfully",
                orderCancellationService.rejectCancellation(principal.getUserId(), id, request)));
    }

    @GetMapping("/cancellations/summary")
    public ResponseEntity<ApiResponse<OrderCancellationSummaryResponse>> getCancellationSummary() {
        return ResponseEntity.ok(ApiResponse.success(
                "Cancellation summary fetched successfully",
                orderCancellationService.getPendingSummary()));
    }

    @PostMapping("/{orderId}/refunds/{refundId}/complete")
    public ResponseEntity<ApiResponse<RefundSummaryResponse>> completeRefund(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String orderId,
            @PathVariable String refundId,
            @Valid @RequestBody CompleteOrderRefundRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Refund completed successfully",
                orderCancellationService.completeRefund(principal.getUserId(), orderId, refundId, request)));
    }

    @PostMapping("/{orderId}/refunds/{refundId}/fail")
    public ResponseEntity<ApiResponse<RefundSummaryResponse>> failRefund(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String orderId,
            @PathVariable String refundId,
            @Valid @RequestBody FailOrderRefundRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Refund failed successfully",
                orderCancellationService.failRefund(principal.getUserId(), orderId, refundId, request)));
    }
}
