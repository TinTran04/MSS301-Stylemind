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