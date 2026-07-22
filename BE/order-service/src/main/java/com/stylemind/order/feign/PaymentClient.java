package com.stylemind.order.feign;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.feign.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@FeignClient(name = "payment-service", url = "${PAYMENT_SERVICE_URL}", configuration = FeignClientConfig.class)
public interface PaymentClient {

    @PostMapping("/internal/v1/payments/cod")
    ApiResponse<PaymentResponse> createCodPayment(@RequestBody CodCheckoutRequest request);

    @PostMapping("/internal/v1/payments/sepay")
    ApiResponse<PaymentResponse> createSepayPayment(@RequestBody SepayCheckoutRequest request);

    @GetMapping("/internal/v1/payments/orders/{orderId}")
    ApiResponse<PaymentResponse> getPaymentStatus(@PathVariable("orderId") String orderId);

    @PostMapping("/internal/v1/payments/orders/{orderId}/expire")
    ApiResponse<Void> expirePaymentByOrderId(@PathVariable("orderId") String orderId);

    @GetMapping("/internal/v1/payments/admin/revenue/sepay")
    ApiResponse<List<PaymentRevenueCandidate>> findSepayRevenueCandidates(
            @RequestParam("from") String from,
            @RequestParam("to") String to);

    @PostMapping("/internal/v1/payments/admin/revenue/by-order-ids")
    ApiResponse<List<PaymentRevenueCandidate>> findRevenueCandidatesByOrderIds(
            @RequestBody RevenueOrderIdsRequest request);

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    class CodCheckoutRequest {
        private String orderId;
        private String userId;
        private BigDecimal amount;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    class SepayCheckoutRequest {
        private String orderId;
        private String userId;
        private BigDecimal amount;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    class PaymentResponse {
        private String transactionId;
        private String status;
        private BigDecimal amount;
        private String method;
        private String transactionRef;
        private String gatewayTransactionId;
        private Instant paidAt;
        private String qrContent;
        private String qrImageUrl;
        private String transferContent;
        private Instant expiresAt;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    class PaymentRevenueCandidate {
        private String orderId;
        private String method;
        private String status;
        private BigDecimal amount;
        private LocalDateTime paidAt;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class RevenueOrderIdsRequest {
        private List<String> orderIds;
    }
}
