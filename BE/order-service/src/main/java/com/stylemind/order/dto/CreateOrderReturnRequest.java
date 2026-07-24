package com.stylemind.order.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderReturnRequest {
    @Size(max = 80)
    private String reasonCode;

    @Size(max = 1000)
    private String customerNote;
}
