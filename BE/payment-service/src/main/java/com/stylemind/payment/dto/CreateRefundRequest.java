package com.stylemind.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRefundRequest {
    @NotBlank
    private String orderId;

    @NotBlank
    private String orderCancellationId;
}
