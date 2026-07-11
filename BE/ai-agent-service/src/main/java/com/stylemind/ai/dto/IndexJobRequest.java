package com.stylemind.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndexJobRequest {
    @NotBlank(message = "targetType is required")
    @Pattern(regexp = "^(PRODUCT|INVENTORY|RULE)$", message = "targetType must be PRODUCT, INVENTORY, or RULE")
    private String targetType;

    @NotBlank(message = "targetId is required")
    private String targetId;

    @NotBlank(message = "operationType is required")
    @Pattern(regexp = "^(CREATE|UPDATE|DELETE)$", message = "operationType must be CREATE, UPDATE, or DELETE")
    private String operationType;
}
