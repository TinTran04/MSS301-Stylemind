package com.stylemind.cart.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cart_items")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "cart_id", length = 50, nullable = false)
    private String cartId;

    @Column(name = "variant_id", length = 50, nullable = false)
    private String variantId;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "is_ai_recommended", nullable = false)
    @Builder.Default
    private Boolean isAiRecommended = false;

    @Column(name = "source_bundle_id", length = 50)
    private String sourceBundleId;

    // Explicit getters to ensure they exist
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
}