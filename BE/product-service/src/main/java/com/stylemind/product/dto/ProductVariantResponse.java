package com.stylemind.product.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantResponse {
    private String id;
    private String productId;
    private String sku;
    private String size;
    private String color;
    private String material;
    private BigDecimal priceOverride;
}