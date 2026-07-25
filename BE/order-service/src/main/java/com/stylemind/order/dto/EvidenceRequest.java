package com.stylemind.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceRequest {

    private String publicId;

    @NotBlank(message = "SECURE_URL_REQUIRED")
    private String secureUrl;

    private String resourceType; // image, video
}
