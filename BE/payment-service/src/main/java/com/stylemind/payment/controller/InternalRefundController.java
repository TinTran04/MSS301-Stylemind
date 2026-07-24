package com.stylemind.payment.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.payment.dto.CompleteRefundRequest;
import com.stylemind.payment.dto.CreateRefundRequest;
import com.stylemind.payment.dto.FailRefundRequest;
import com.stylemind.payment.dto.RefundResponse;
import com.stylemind.payment.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/refunds")
@RequiredArgsConstructor
public class InternalRefundController {

    private final RefundService refundService;

    @PostMapping
    public ResponseEntity<ApiResponse<RefundResponse>> createRefund(@Valid @RequestBody CreateRefundRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Refund pending", refundService.createPendingRefund(request)));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<RefundResponse>> getRefundByOrder(@PathVariable String orderId) {
        RefundResponse refund = refundService.getRefundByOrderId(orderId);
        if (refund == null) {
            throw new com.stylemind.common.exception.BusinessException("REFUND_NOT_FOUND", "Refund transaction not found for order", 404);
        }
        return ResponseEntity.ok(ApiResponse.success("OK", refund));
    }

    @PostMapping("/{refundId}/complete")
    public ResponseEntity<ApiResponse<RefundResponse>> completeRefund(
            @PathVariable String refundId,
            @Valid @RequestBody CompleteRefundRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Refund completed", refundService.completeRefund(refundId, request)));
    }

    @PostMapping("/{refundId}/fail")
    public ResponseEntity<ApiResponse<RefundResponse>> failRefund(
            @PathVariable String refundId,
            @Valid @RequestBody FailRefundRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Refund failed", refundService.failRefund(refundId, request)));
    }
}
