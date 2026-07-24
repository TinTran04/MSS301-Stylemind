package com.stylemind.payment.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.payment.dto.PayoutDestinationRequest;
import com.stylemind.payment.dto.PayoutDestinationResponse;
import com.stylemind.payment.service.impl.RefundServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
public class PayoutController {

    private final RefundServiceImpl refundService;

    @PutMapping("/{returnRequestId}/payout-destination")
    public ResponseEntity<ApiResponse<PayoutDestinationResponse>> savePayoutDestination(
            @PathVariable("returnRequestId") String returnRequestId,
            @Valid @RequestBody PayoutDestinationRequest request) {
        PayoutDestinationResponse response = refundService.savePayoutDestination(returnRequestId, request);
        return ResponseEntity.ok(ApiResponse.success("STK ngân hàng đã được lưu thành công", response));
    }
}
