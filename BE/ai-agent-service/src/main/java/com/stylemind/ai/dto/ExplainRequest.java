package com.stylemind.ai.dto;

import lombok.*;
import jakarta.validation.constraints.NotBlank;
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
