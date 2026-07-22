package com.stylemind.payment.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.payment.dto.CodCheckoutRequest;
import com.stylemind.payment.dto.PaymentResponse;
import com.stylemind.payment.dto.PaymentRevenueCandidate;
import com.stylemind.payment.dto.RevenueOrderIdsRequest;
import com.stylemind.payment.dto.SepayCheckoutRequest;
import com.stylemind.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

// Service-to-service only - order-service (the checkout orchestrator) is the sole
// caller. Guarded by InternalAuthFilter/SecurityConfig's /internal/v1/** rules and
// unreachable through the gateway (see api-gateway's internal-block route).
@RestController
@RequestMapping("/internal/v1/payments")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/cod")
    public ResponseEntity<ApiResponse<PaymentResponse>> createCodPayment(@Valid @RequestBody CodCheckoutRequest request) {
        PaymentResponse response = paymentService.createCodPayment(request);
        return ResponseEntity.ok(ApiResponse.success("COD payment recorded", response));
    }

    @PostMapping("/sepay")
    public ResponseEntity<ApiResponse<PaymentResponse>> createSepayPayment(@Valid @RequestBody SepayCheckoutRequest request) {
        PaymentResponse response = paymentService.createSepayPayment(request);
        return ResponseEntity.ok(ApiResponse.success("SePay payment initialized", response));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentStatus(@PathVariable String orderId) {
        PaymentResponse response = paymentService.getPaymentStatus(orderId);
        return ResponseEntity.ok(ApiResponse.success("OK", response));
    }

    @PostMapping("/orders/{orderId}/expire")
    public ResponseEntity<ApiResponse<Void>> expirePayment(@PathVariable String orderId) {
        paymentService.expirePendingSepayPayment(orderId);
        return ResponseEntity.ok(ApiResponse.success("Payment expired", null));
    }

    @PostMapping("/{transactionId}/refund")
    public ResponseEntity<ApiResponse<Void>> refund(@PathVariable String transactionId) {
        paymentService.refund(transactionId);
        return ResponseEntity.ok(ApiResponse.success("Refund processed", null));
    }

    @GetMapping("/admin/revenue/sepay")
    public ResponseEntity<ApiResponse<List<PaymentRevenueCandidate>>> findSepayRevenueCandidates(
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to) {
        return ResponseEntity.ok(ApiResponse.success("OK", paymentService.findSepayRevenueCandidates(from, to)));
    }

    @PostMapping("/admin/revenue/by-order-ids")
    public ResponseEntity<ApiResponse<List<PaymentRevenueCandidate>>> findRevenueCandidatesByOrderIds(
            @Valid @RequestBody RevenueOrderIdsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "OK", paymentService.findRevenueCandidatesByOrderIds(request.getOrderIds())));
    }
}
