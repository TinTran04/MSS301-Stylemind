package com.stylemind.order.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnItemResponse {
    private String id;
    private String orderItemId;
    private String productId;
    private String variantId;
    private Integer quantity;
    private String restockStatus;
}
