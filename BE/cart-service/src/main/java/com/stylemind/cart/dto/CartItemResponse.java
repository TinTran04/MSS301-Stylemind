package com.stylemind.cart.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {
    private String id;
    private String cartId;
    private String variantId;
    private Integer quantity;
    private Boolean isAiRecommended;
    private String sourceBundleId;
    private VariantInfo variant;
    private java.time.Instant addedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCartId() { return cartId; }
    public void setCartId(String cartId) { this.cartId = cartId; }
    public String getVariantId() { return variantId; }
    public void setVariantId(String variantId) { this.variantId = variantId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Boolean getIsAiRecommended() { return isAiRecommended; }
    public void setIsAiRecommended(Boolean isAiRecommended) { this.isAiRecommended = isAiRecommended; }
    public String getSourceBundleId() { return sourceBundleId; }
    public void setSourceBundleId(String sourceBundleId) { this.sourceBundleId = sourceBundleId; }
    public VariantInfo getVariant() { return variant; }
    public void setVariant(VariantInfo variant) { this.variant = variant; }
    public java.time.Instant getAddedAt() { return addedAt; }
    public void setAddedAt(java.time.Instant addedAt) { this.addedAt = addedAt; }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VariantInfo {
        private String id;
        private String sku;
        private String size;
        private String color;
        private String material;
        private BigDecimal priceOverride;
        private ProductInfo product;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
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
        public ProductInfo getProduct() { return product; }
        public void setProduct(ProductInfo product) { this.product = product; }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class ProductInfo {
            private String id;
            private String name;
            private BigDecimal basePrice;
            private List<ImageInfo> images;

            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public BigDecimal getBasePrice() { return basePrice; }
            public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
            public List<ImageInfo> getImages() { return images; }
            public void setImages(List<ImageInfo> images) { this.images = images; }

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @Builder
            public static class ImageInfo {
                private String imageUrl;
                private Boolean isPrimary;

                public String getImageUrl() { return imageUrl; }
                public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
                public Boolean getIsPrimary() { return isPrimary; }
                public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
            }
        }
    }
}