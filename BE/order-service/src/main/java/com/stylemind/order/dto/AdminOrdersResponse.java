package com.stylemind.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrdersResponse {
    private Page<OrderResponse> page;
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
}
