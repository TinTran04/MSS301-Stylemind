package com.stylemind.order.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {
    @NotBlank(message = "Variant ID không được để trống")
    private String variantId;

    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
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