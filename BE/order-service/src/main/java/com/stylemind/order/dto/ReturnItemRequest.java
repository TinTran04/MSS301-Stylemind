package com.stylemind.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnItemRequest {

    @NotNull(message = "ORDER_ITEM_ID_REQUIRED")
    private String orderItemId;

    @NotNull(message = "QUANTITY_REQUIRED")
    @Min(value = 1, message = "QUANTITY_INVALID")
    private Integer quantity;
}
