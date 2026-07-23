package com.stylemind.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompleteRefundRequest {
    @NotBlank
    private String providerReference;

    private String proofUrl;
    private String note;

    @NotBlank
    private String processedBy;
}
