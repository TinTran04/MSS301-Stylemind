package com.stylemind.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantSnapshotResponse {
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
}
