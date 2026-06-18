package com.stylemind.order.feign;

import com.stylemind.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@FeignClient(name = "payment-service", url = "${PAYMENT_SERVICE_URL:http://localhost:8088}")
public interface PaymentClient {

    @PostMapping("/api/payment/checkout")
    ApiResponse<PaymentResponse> checkout(@RequestBody CheckoutRequest request);

    // Internal endpoints
    @PostMapping("/internal/payment/process")
    ApiResponse<PaymentResponse> processPayment(@RequestBody ProcessPaymentRequest request);

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    class CheckoutRequest {
        private String orderId;
        private String method;
        private BigDecimal amount;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    class ProcessPaymentRequest {
        private String transactionId;
        private String orderId;
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
    }
}