package com.stylemind.user.dto;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StyleProfileResponse {
    private String userId;
    private String gender;
    private Integer age;
    private java.math.BigDecimal heightCm;
    private java.math.BigDecimal weightKg;
    private String bodyMorphology;
    private String preferredFit;
    private String stylePersonas;
    private Instant createdAt;
    private Instant updatedAt;
}