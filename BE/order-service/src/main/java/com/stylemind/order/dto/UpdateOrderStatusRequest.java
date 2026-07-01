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
public class UpdateOrderStatusRequest {
    @NotBlank(message = "Order status is required")
    @Pattern(
            regexp = "^(PENDING|PROCESSING|COMPENSATING_ROLLBACK|FULFILLED|CANCELLED)$",
            message = "Order status is invalid"
    )
    private String orderStatus;

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
}
