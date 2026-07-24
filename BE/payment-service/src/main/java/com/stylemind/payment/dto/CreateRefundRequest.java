package com.stylemind.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRefundRequest {
    @NotBlank
    private String orderId;

    private String orderCancellationId;

    private String returnRequestId;

    private BigDecimal merchandiseAmount;

    private BigDecimal taxAmount;

    private BigDecimal shippingAmount;

    private String reason;
}
