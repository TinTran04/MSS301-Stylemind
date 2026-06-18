package com.stylemind.order.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "order_id", length = 50, nullable = false)
    private String orderId;

    @Column(name = "variant_id", length = 50, nullable = false)
    private String variantId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "price_at_purchase", precision = 12, scale = 2, nullable = false)
    private java.math.BigDecimal priceAtPurchase;

    @Column(name = "is_ai_conversion", nullable = false)
    @Builder.Default
    private Boolean isAiConversion = false;

    @Column(name = "source_bundle_id", length = 50)
    private String sourceBundleId;

    // Explicit getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getVariantId() { return variantId; }
    public void setVariantId(String variantId) { this.variantId = variantId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public java.math.BigDecimal getPriceAtPurchase() { return priceAtPurchase; }
    public void setPriceAtPurchase(java.math.BigDecimal priceAtPurchase) { this.priceAtPurchase = priceAtPurchase; }
    public Boolean getIsAiConversion() { return isAiConversion; }
    public void setIsAiConversion(Boolean isAiConversion) { this.isAiConversion = isAiConversion; }
    public String getSourceBundleId() { return sourceBundleId; }
    public void setSourceBundleId(String sourceBundleId) { this.sourceBundleId = sourceBundleId; }
}