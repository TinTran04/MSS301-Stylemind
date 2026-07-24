package com.stylemind.order.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnEvidenceResponse {
    private String id;
    private String publicId;
    private String secureUrl;
    private String resourceType;
    private LocalDateTime uploadedAt;
}
