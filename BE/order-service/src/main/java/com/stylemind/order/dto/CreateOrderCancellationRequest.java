package com.stylemind.order.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderCancellationRequest {
    private String reasonCode;

    @Size(max = 1000)
    private String customerNote;
}
