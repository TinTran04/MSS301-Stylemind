package com.stylemind.order.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.dto.PageResponse;
import com.stylemind.common.security.UserPrincipal;
import com.stylemind.common.web.PaginationSupport;
import com.stylemind.order.dto.*;
import com.stylemind.order.entity.CustomerCancellationReason;
import com.stylemind.order.service.OrderService;
import com.stylemind.order.service.OrderCancellationService;
import com.stylemind.order.service.OrderReturnService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderCancellationService orderCancellationService;
    private final OrderReturnService orderReturnService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest orderRequest) {
        String authHeader = request.getHeader("Authorization");
        OrderResponse order = orderService.createOrder(principal.getUserId(), authHeader, idempotencyKey, orderRequest);
        return ResponseEntity.ok(ApiResponse.success("Order created successfully", order));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> getOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String status) {
        Pageable pageable = PaginationSupport.customerListPageable(page, size, sort);
        PageResponse<OrderSummaryResponse> orders = orderService.getOrdersPage(principal.getUserId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Orders fetched successfully", orders));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String orderId) {
        OrderResponse order = orderService.getOrder(principal.getUserId(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Order fetched successfully", order));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String orderId,
            @RequestBody(required = false) CreateOrderCancellationRequest request) {
        CreateOrderCancellationRequest effectiveRequest = request != null ? request : CreateOrderCancellationRequest.builder()
                .reasonCode(CustomerCancellationReason.OTHER.name())
                .customerNote("Khách hàng hủy đơn từ luồng cũ.")
                .build();
        orderCancellationService.requestCustomerCancellation(principal.getUserId(), orderId, null, effectiveRequest);
        OrderResponse order = orderService.getOrder(principal.getUserId(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", order));
    }

    @PostMapping("/{orderId}/cancellations")
    public ResponseEntity<ApiResponse<OrderCancellationResponse>> createCancellation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String orderId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderCancellationRequest request) {
        OrderCancellationResponse response = orderCancellationService.requestCustomerCancellation(
                principal.getUserId(), orderId, idempotencyKey, request);
        return ResponseEntity.ok(ApiResponse.success("Cancellation requested successfully", response));
    }

    @GetMapping("/{orderId}/cancellations")
    public ResponseEntity<ApiResponse<java.util.List<OrderCancellationResponse>>> getCancellations(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cancellation history fetched successfully",
                orderCancellationService.getCustomerCancellations(principal.getUserId(), orderId)));
    }

    @PostMapping(value = "/{orderId}/return-requests", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<OrderReturnRequestResponse>> createReturnRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String orderId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestParam(required = false) String reasonCode,
            @RequestParam(required = false) String customerNote,
            @RequestPart(value = "images", required = false) MultipartFile[] images) {
        OrderReturnRequestResponse response = orderReturnService.requestCustomerReturn(
                principal.getUserId(),
                orderId,
                idempotencyKey,
                CreateOrderReturnRequest.builder().reasonCode(reasonCode).customerNote(customerNote).build(),
                images == null ? java.util.List.of() : java.util.Arrays.asList(images));
        return ResponseEntity.ok(ApiResponse.success("Return request created successfully", response));
    }

    @GetMapping("/{orderId}/return-requests")
    public ResponseEntity<ApiResponse<java.util.List<OrderReturnRequestResponse>>> getReturnRequests(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Return requests fetched successfully",
                orderReturnService.getCustomerReturns(principal.getUserId(), orderId)));
    }

    @PatchMapping("/{orderId}/return-requests/{returnRequestId}/bank-info")
    public ResponseEntity<ApiResponse<OrderReturnRequestResponse>> submitReturnBankInfo(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String orderId,
            @PathVariable String returnRequestId,
            @Valid @RequestBody SubmitReturnBankInfoRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Bank information submitted successfully",
                orderReturnService.submitBankInfo(principal.getUserId(), orderId, returnRequestId, request)));
    }

    @PostMapping(value = "/{orderId}/delivery-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<OrderResponse>> uploadDeliveryImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String orderId,
            @RequestPart("file") MultipartFile file) {
        OrderResponse order = orderService.uploadDeliveryImage(principal.getUserId(), orderId, file);
        return ResponseEntity.ok(ApiResponse.success("Delivery image uploaded successfully", order));
    }

}
