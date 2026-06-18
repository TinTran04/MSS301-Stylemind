package com.stylemind.payment.controller;

import com.stylemind.payment.dto.*;
import com.stylemind.payment.service.PaymentService;
import com.stylemind.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.stylemind.common.security.UserPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<PaymentResponse>> checkout(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CheckoutRequest request) {
        PaymentResponse response = paymentService.checkout(request);
        return ResponseEntity.ok(ApiResponse.success("Khởi tạo thanh toán thành công", response));
    }

    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(ApiResponse.success("Xử lý thanh toán thành công", response));
    }

    @PostMapping("/{transactionId}/refund")
    public ResponseEntity<ApiResponse<Void>> refund(
            @PathVariable String transactionId) {
        paymentService.refund(transactionId);
        return ResponseEntity.ok(ApiResponse.success("Hoàn tiền thành công", null));
    }
}