package com.stylemind.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryResponse {
    private String id;
    private Instant createdAt;
    private String orderStatus;
    private BigDecimal totalAmount;
    private int itemCount;
    private OrderCancellationResponse latestCancellation;
    private Boolean hasPendingCancellation;
}
