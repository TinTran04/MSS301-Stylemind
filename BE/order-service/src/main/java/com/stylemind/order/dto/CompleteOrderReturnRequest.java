package com.stylemind.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompleteOrderReturnRequest {
    @NotBlank
    @Size(max = 150)
    private String refundReference;

    @Size(max = 1000)
    private String refundNote;
}
