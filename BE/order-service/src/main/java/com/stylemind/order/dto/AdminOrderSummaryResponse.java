package com.stylemind.order.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Real, aggregated order metrics for the admin dashboard. Counts/sums only —
 * no per-order or sensitive data is exposed. Revenue counts only orders whose
 * payment has been received and are progressing/done (see OrderService).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderSummaryResponse {
    private long totalOrders;
    private long pendingOrders;    // PENDING + PAYMENT_PENDING
    private long paidOrders;       // PAID
    private long completedOrders;  // COMPLETED
    private long cancelledOrders;  // CANCELLED + EXPIRED + FAILED
    private long todayOrders;
    private BigDecimal totalRevenue;
    private BigDecimal todayRevenue;
}
