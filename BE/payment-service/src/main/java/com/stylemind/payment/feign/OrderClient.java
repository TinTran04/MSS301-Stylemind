package com.stylemind.payment.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "order-service", url = "${ORDER_SERVICE_URL}")
public interface OrderClient {

    @PostMapping("/internal/v1/orders/{orderId}/payment-status")
    void updatePaymentStatus(@PathVariable("orderId") String orderId, @RequestBody PaymentStatusUpdateRequest request);

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    class PaymentStatusUpdateRequest {
        private String status;
    }
}
