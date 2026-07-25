package com.stylemind.order.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {
    private String id;
    private String orderId;
    private String variantId;
    private String catalogVariantId;
    private String productId;
    private String productName;
    private String sku;
    private String size;
    private String color;
    private String material;
    private String primaryImageUrl;
    private Integer quantity;
    private BigDecimal priceAtPurchase;
    private Boolean isAiConversion;
    private String sourceBundleId;
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getVariantId() { return variantId; }
    public void setVariantId(String variantId) { this.variantId = variantId; }
    public String getCatalogVariantId() { return catalogVariantId; }
    public void setCatalogVariantId(String catalogVariantId) { this.catalogVariantId = catalogVariantId; }
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
    public String getPrimaryImageUrl() { return primaryImageUrl; }
    public void setPrimaryImageUrl(String primaryImageUrl) { this.primaryImageUrl = primaryImageUrl; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getPriceAtPurchase() { return priceAtPurchase; }
    public void setPriceAtPurchase(BigDecimal priceAtPurchase) { this.priceAtPurchase = priceAtPurchase; }
    public Boolean getIsAiConversion() { return isAiConversion; }
    public void setIsAiConversion(Boolean isAiConversion) { this.isAiConversion = isAiConversion; }
    public String getSourceBundleId() { return sourceBundleId; }
    public void setSourceBundleId(String sourceBundleId) { this.sourceBundleId = sourceBundleId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
