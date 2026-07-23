package com.stylemind.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompleteOrderRefundRequest {
    @NotBlank
    private String providerReference;
    private String proofUrl;
    private String note;
}
