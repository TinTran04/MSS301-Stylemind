package com.stylemind.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Safe, internal-only payment facts used by order-service revenue recognition. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRevenueCandidate {
    private String orderId;
    private String method;
    private String status;
    private BigDecimal amount;
    private LocalDateTime paidAt;
}
