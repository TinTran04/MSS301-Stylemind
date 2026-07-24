package com.stylemind.order.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.security.UserPrincipal;
import com.stylemind.order.dto.AdminCancelOrderRequest;
import com.stylemind.order.dto.AdminOrdersResponse;
import com.stylemind.order.dto.AdminOrderSummaryResponse;
import com.stylemind.order.dto.CompleteOrderRefundRequest;
import com.stylemind.order.dto.CompleteOrderReturnRequest;
import com.stylemind.order.dto.FailOrderRefundRequest;
import com.stylemind.order.dto.OrderCancellationResponse;
import com.stylemind.order.dto.OrderCancellationSummaryResponse;
import com.stylemind.order.dto.OrderResponse;
import com.stylemind.order.dto.RejectOrderCancellationRequest;
import com.stylemind.order.dto.RejectOrderReturnRequest;
import com.stylemind.order.dto.UpdateOrderStatusRequest;
import com.stylemind.order.dto.RefundSummaryResponse;
import com.stylemind.order.dto.OrderReturnRequestResponse;
import com.stylemind.order.service.OrderCancellationService;
import com.stylemind.order.service.OrderReturnService;
import com.stylemind.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;
    private final OrderCancellationService orderCancellationService;
    private final OrderReturnService orderReturnService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminOrdersResponse>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String cancellationStatus,
            @RequestParam(required = false) String returnStatus,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        AdminOrdersResponse result = orderService.getAllOrdersForAdmin(
                status,
                cancellationStatus,
                returnStatus,
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

    @PatchMapping("/return-requests/{id}/approve")
    public ResponseEntity<ApiResponse<OrderReturnRequestResponse>> approveReturnRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Return request approved successfully",
                orderReturnService.approveReturn(principal.getUserId(), id)));
    }

    @PatchMapping(value = "/return-requests/{id}/reject", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<OrderReturnRequestResponse>> rejectReturnRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @RequestParam String rejectionReason,
            @RequestPart(value = "images", required = false) MultipartFile[] images) {
        return ResponseEntity.ok(ApiResponse.success(
                "Return request rejected successfully",
                orderReturnService.rejectReturn(
                        principal.getUserId(),
                        id,
                        RejectOrderReturnRequest.builder().rejectionReason(rejectionReason).build(),
                        images == null ? java.util.List.of() : java.util.Arrays.asList(images))));
    }

    @PostMapping(value = "/{orderId}/return-requests/{id}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<OrderReturnRequestResponse>> completeReturnRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String orderId,
            @PathVariable String id,
            @RequestParam String refundReference,
            @RequestParam(required = false) String refundNote,
            @RequestPart(value = "billImages", required = false) MultipartFile[] billImages) {
        return ResponseEntity.ok(ApiResponse.success(
                "Return refund completed successfully",
                orderReturnService.completeReturn(
                        principal.getUserId(),
                        orderId,
                        id,
                        CompleteOrderReturnRequest.builder().refundReference(refundReference).refundNote(refundNote).build(),
                        billImages == null ? java.util.List.of() : java.util.Arrays.asList(billImages))));
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
