package com.stylemind.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectOrderReturnRequest {
    @NotBlank
    @Size(max = 1000)
    private String rejectionReason;
}
