package com.stylemind.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {
    @NotBlank(message = "Variant ID is required")
    private String variantId;

    @Min(value = 1, message = "Quantity must be greater than 0")
    private Integer quantity;

    private Boolean isAiConversion = false;

    private String sourceBundleId;

    public String getVariantId() { return variantId; }
    public void setVariantId(String variantId) { this.variantId = variantId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Boolean getIsAiConversion() { return isAiConversion; }
    public void setIsAiConversion(Boolean isAiConversion) { this.isAiConversion = isAiConversion; }
    public String getSourceBundleId() { return sourceBundleId; }
    public void setSourceBundleId(String sourceBundleId) { this.sourceBundleId = sourceBundleId; }
}
