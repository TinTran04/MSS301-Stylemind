package com.stylemind.ai.feign;

import com.stylemind.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "product-service", url = "${PRODUCT_SERVICE_URL:http://localhost:8083}")
public interface ProductClient {

    @GetMapping("/internal/products/{id}")
    ApiResponse<ProductDetail> getProduct(@PathVariable String id);

    @GetMapping("/internal/products/variants")
    ApiResponse<List<VariantDetail>> getVariants(@RequestParam List<String> variantIds);

    @GetMapping("/internal/products/by-skus")
    ApiResponse<List<ProductDetail>> getProductsBySkus(@RequestParam String skus);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    class ProductDetail {
        private String id;
        private String name;
        private String description;
        private BigDecimal basePrice;
        private String aestheticStyle;
        private String targetDemographic;
        private String seasonalProperty;
        private String status;
        private List<ImageDetail> images;
        private List<VariantDetail> variants;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    class VariantDetail {
        private String id;
        private String productId;
        private String sku;
        private String size;
        private String color;
        private String material;
        private BigDecimal priceOverride;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    class ImageDetail {
        private String imageUrl;
        private Boolean isPrimary;
    }
}