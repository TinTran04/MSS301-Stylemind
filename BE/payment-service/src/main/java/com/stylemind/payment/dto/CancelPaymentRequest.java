package com.stylemind.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelPaymentRequest {
    @NotBlank
    private String orderCancellationId;

    public String getOrderCancellationId() { return orderCancellationId; }
    public void setOrderCancellationId(String orderCancellationId) { this.orderCancellationId = orderCancellationId; }
}
