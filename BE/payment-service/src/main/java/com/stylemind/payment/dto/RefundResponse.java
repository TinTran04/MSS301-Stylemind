package com.stylemind.payment.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponse {
    private String id;
    private String orderId;
    private String paymentTransactionId;
    private String orderCancellationId;
    private String returnRequestId;
    private String bankCode;
    private String accountHolder;
    private String maskedAccountNumber;
    private BigDecimal amount;
    private String status;
    private String method;
    private String providerReference;
    private String proofUrl;
    private String note;
    private String processedBy;
    private Instant processedAt;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
}
