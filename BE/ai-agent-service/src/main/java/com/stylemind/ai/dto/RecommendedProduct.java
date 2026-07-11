package com.stylemind.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// The stable contract between AI recommendations and "add to cart from recommendation"
// (CART-08 / ORDER-06). Keep this shape backward compatible even when real AI/vector
// search replaces the current rule-based matching - the frontend and cart wiring depend
// on productId/basePrice/imageUrl staying put.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendedProduct {
    private String productId;
    private String name;
    private BigDecimal basePrice;
    private String imageUrl;
    private String reason;
    private Double matchScore;
}
