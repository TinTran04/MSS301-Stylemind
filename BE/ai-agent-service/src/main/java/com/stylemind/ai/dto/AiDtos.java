package com.stylemind.ai.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExplainRequest {
    @NotBlank(message = "Product ID không được để trống")
    private String productId;

    private UserContext userContext;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserContext {
        private String bodyMorphology;
        private List<String> stylePersonas;
        private List<String> preferredColors;
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendOutfitsRequest {
    @NotBlank(message = "Dịp sử dụng không được để trống")
    private String occasion;

    @NotBlank(message = "Phong cách không được để trống")
    private String style;

    @NotBlank(message = "Giới tính không được để trống")
    private String gender;

    private BigDecimal budget;

    private List<String> preferredColors;
}