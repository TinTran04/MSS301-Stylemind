package com.stylemind.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailRefundRequest {
    @NotBlank
    private String failureReason;

    @NotBlank
    private String processedBy;
}
