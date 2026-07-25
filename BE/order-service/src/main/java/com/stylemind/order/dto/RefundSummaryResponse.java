package com.stylemind.order.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundSummaryResponse {
    private String id;
    private String orderId;
    private String paymentTransactionId;
    private String orderCancellationId;
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
