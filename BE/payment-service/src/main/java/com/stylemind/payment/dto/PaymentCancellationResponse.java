package com.stylemind.payment.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCancellationResponse {
    private String transactionId;
    private String orderId;
    private String status;
    private String method;
    private BigDecimal amount;
    private boolean paymentReceived;
    private String orderCancellationId;
}
