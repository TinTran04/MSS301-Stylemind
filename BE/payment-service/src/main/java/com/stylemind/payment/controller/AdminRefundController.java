package com.stylemind.payment.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.payment.dto.CompleteRefundRequest;
import com.stylemind.payment.dto.FailRefundRequest;
import com.stylemind.payment.dto.RefundResponse;
import com.stylemind.payment.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/refunds")
@RequiredArgsConstructor
public class AdminRefundController {

    private final RefundService refundService;

    @PostMapping("/{refundId}/complete")
    public ResponseEntity<ApiResponse<RefundResponse>> completeRefund(
            @PathVariable("refundId") String refundId,
            @Valid @RequestBody CompleteRefundRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Refund completed", refundService.completeRefund(refundId, request)));
    }

    @PostMapping("/{refundId}/fail")
    public ResponseEntity<ApiResponse<RefundResponse>> failRefund(
            @PathVariable("refundId") String refundId,
            @Valid @RequestBody FailRefundRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Refund failed", refundService.failRefund(refundId, request)));
    }
}
