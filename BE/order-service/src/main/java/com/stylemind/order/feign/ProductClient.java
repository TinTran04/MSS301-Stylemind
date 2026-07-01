package com.stylemind.order.feign;

import com.stylemind.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@FeignClient(name = "product-service", url = "${PRODUCT_SERVICE_URL:http://localhost:8083}")
public interface ProductClient {

    @GetMapping("/internal/products/variants/{variantId}")
    ApiResponse<VariantSnapshot> getVariantSnapshot(@PathVariable String variantId);

    class VariantSnapshot {
        private String variantId;
        private String productId;
        private String productName;
        private String sku;
        private String size;
        private String color;
        private String material;
        private BigDecimal effectivePrice;
        private String status;
        private String primaryImageUrl;

        public String getVariantId() { return variantId; }
        public void setVariantId(String variantId) { this.variantId = variantId; }
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public String getMaterial() { return material; }
        public void setMaterial(String material) { this.material = material; }
        public BigDecimal getEffectivePrice() { return effectivePrice; }
        public void setEffectivePrice(BigDecimal effectivePrice) { this.effectivePrice = effectivePrice; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getPrimaryImageUrl() { return primaryImageUrl; }
        public void setPrimaryImageUrl(String primaryImageUrl) { this.primaryImageUrl = primaryImageUrl; }
    }
}
