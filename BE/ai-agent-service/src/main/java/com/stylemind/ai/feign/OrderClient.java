package com.stylemind.ai.feign;

import com.stylemind.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "order-service", url = "${ORDER_SERVICE_URL:http://localhost:8087}")
public interface OrderClient {

    @GetMapping("/internal/orders/{id}")
    ApiResponse<OrderDetail> getOrder(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    class OrderDetail {
        private String id;
        private String userId;
        private String orderStatus;
        private String shippingAddress;
        private java.time.Instant createdAt;
        private List<OrderItemDetail> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    class OrderItemDetail {
        private String variantId;
        private Integer quantity;
        private java.math.BigDecimal priceAtPurchase;
    }
}