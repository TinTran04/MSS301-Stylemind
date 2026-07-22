package com.stylemind.order.dto;

import java.math.BigDecimal;

public record OrderRevenueAggregate(
        BigDecimal netRevenue,
        BigDecimal vatCollected,
        BigDecimal shippingFeesCollected,
        BigDecimal grossCustomerPayments,
        long orderCount) {

    public static OrderRevenueAggregate zero() {
        return new OrderRevenueAggregate(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
    }
}
