package com.stylemind.order.feign;

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

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getBasePrice() { return basePrice; }
        public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
        public String getAestheticStyle() { return aestheticStyle; }
        public void setAestheticStyle(String aestheticStyle) { this.aestheticStyle = aestheticStyle; }
        public String getTargetDemographic() { return targetDemographic; }
        public void setTargetDemographic(String targetDemographic) { this.targetDemographic = targetDemographic; }
        public String getSeasonalProperty() { return seasonalProperty; }
        public void setSeasonalProperty(String seasonalProperty) { this.seasonalProperty = seasonalProperty; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public List<ImageDetail> getImages() { return images; }
        public void setImages(List<ImageDetail> images) { this.images = images; }
        public List<VariantDetail> getVariants() { return variants; }
        public void setVariants(List<VariantDetail> variants) { this.variants = variants; }
    }

    class VariantDetail {
        private String id;
        private String productId;
        private String sku;
        private String size;
        private String color;
        private String material;
        private BigDecimal priceOverride;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public String getMaterial() { return material; }
        public void setMaterial(String material) { this.material = material; }
        public BigDecimal getPriceOverride() { return priceOverride; }
        public void setPriceOverride(BigDecimal priceOverride) { this.priceOverride = priceOverride; }
    }

    class ImageDetail {
        private String imageUrl;
        private Boolean isPrimary;

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public Boolean getIsPrimary() { return isPrimary; }
        public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
    }
}