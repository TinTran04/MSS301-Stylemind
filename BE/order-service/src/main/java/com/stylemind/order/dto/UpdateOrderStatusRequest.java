package com.stylemind.order.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderStatusRequest {
    @NotBlank(message = "Trạng thái không được để trống")
    @Pattern(regexp = "^(PENDING|PROCESSING|COMPENSATING_ROLLBACK|FULFILLED|CANCELLED)$", message = "Trạng thái không hợp lệ")
    private String orderStatus;

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
}