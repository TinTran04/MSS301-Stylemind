package com.stylemind.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {
    @NotBlank(message = "Shipping address ID is required")
    private String addressId;

    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "^(cod|sepay)$", message = "Payment method is invalid")
    private String paymentMethod;

    // Deprecated/ignored: checkout is orchestrated by order-service and payment
    // transactions are created server-side. Kept only for backward compatibility.
    @Deprecated
    private String transactionId;

    public String getAddressId() { return addressId; }
    public void setAddressId(String addressId) { this.addressId = addressId; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
}
