package com.stylemind.ai.feign;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.dto.PageResponse;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@FeignClient(name = "product-service", url = "${PRODUCT_SERVICE_URL:http://localhost:8083}")
public interface ProductClient {

    @GetMapping("/api/v1/products")
    ApiResponse<PageResponse<ProductSummary>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size);

    @GetMapping("/api/v1/products/{id}")
    ApiResponse<ProductSummary> getProduct(@PathVariable String id);

    @Data
    class ProductSummary {
        private String id;
        private String name;
        private String description;
        private BigDecimal basePrice;
        private String targetDemographic;
        private String status;
        private List<ImageSummary> images;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    class ImageSummary {
        private String imageUrl;
        private Boolean isPrimary;
    }
}
