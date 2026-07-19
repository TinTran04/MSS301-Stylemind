package com.stylemind.order.feign;

import com.stylemind.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;

@FeignClient(name = "payment-service", url = "${PAYMENT_SERVICE_URL:http://localhost:8088}")
public interface PaymentClient {

    @PostMapping("/internal/v1/payments/cod")
    ApiResponse<PaymentResponse> createCodPayment(@RequestBody CodCheckoutRequest request);

    @PostMapping("/internal/v1/payments/sepay")
    ApiResponse<PaymentResponse> createSepayPayment(@RequestBody SepayCheckoutRequest request);

    @GetMapping("/internal/v1/payments/orders/{orderId}")
    ApiResponse<PaymentResponse> getPaymentStatus(@PathVariable("orderId") String orderId);

    @PostMapping("/internal/v1/payments/orders/{orderId}/expire")
    ApiResponse<Void> expirePaymentByOrderId(@PathVariable("orderId") String orderId);

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
}
