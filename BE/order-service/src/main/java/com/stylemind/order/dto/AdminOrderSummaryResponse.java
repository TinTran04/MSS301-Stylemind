package com.stylemind.order.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Real, aggregated order metrics for the admin dashboard. Counts/sums only —
 * no per-order or sensitive data is exposed. Revenue uses payment recognition
 * rules from AdminRevenueService rather than order status alone.
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
    /** @deprecated Use netRevenue. Kept for existing admin clients. */
    private BigDecimal totalRevenue;
    private BigDecimal netRevenue;
    private BigDecimal vatCollected;
    private BigDecimal shippingFeesCollected;
    private BigDecimal grossCustomerPayments;
    private BigDecimal refundAmount;
    private long recognizedOrderCount;
    private BigDecimal sepayRecognizedRevenue;
    private BigDecimal codRecognizedRevenue;
    private String currency;
    private BigDecimal todayNetRevenue;
    private BigDecimal todayVatCollected;
    private BigDecimal todayShippingFeesCollected;
    private BigDecimal todayGrossCustomerPayments;
    private BigDecimal todayRefundAmount;
    private long todayRecognizedOrderCount;
    private BigDecimal todayRevenue;
}
