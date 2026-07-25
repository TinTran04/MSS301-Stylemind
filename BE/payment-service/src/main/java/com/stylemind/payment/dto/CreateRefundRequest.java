package com.stylemind.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRefundRequest {
    @NotBlank
    private String orderId;

    private String orderCancellationId;

    private String returnRequestId;

    private BigDecimal merchandiseAmount;

    private BigDecimal taxAmount;

    private BigDecimal shippingAmount;

    private String reason;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getOrderCancellationId() { return orderCancellationId; }
    public void setOrderCancellationId(String orderCancellationId) { this.orderCancellationId = orderCancellationId; }
    public String getReturnRequestId() { return returnRequestId; }
    public void setReturnRequestId(String returnRequestId) { this.returnRequestId = returnRequestId; }
    public BigDecimal getMerchandiseAmount() { return merchandiseAmount; }
    public void setMerchandiseAmount(BigDecimal merchandiseAmount) { this.merchandiseAmount = merchandiseAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getShippingAmount() { return shippingAmount; }
    public void setShippingAmount(BigDecimal shippingAmount) { this.shippingAmount = shippingAmount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
