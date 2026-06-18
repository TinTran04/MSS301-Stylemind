package com.stylemind.product.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private String id;
    private Long categoryId;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private String aestheticStyle;
    private String targetDemographic;
    private String seasonalProperty;
    private String status;
    private List<ProductImageResponse> images;
    private List<ProductVariantResponse> variants;
    private Instant createdAt;
    private Instant updatedAt;
}