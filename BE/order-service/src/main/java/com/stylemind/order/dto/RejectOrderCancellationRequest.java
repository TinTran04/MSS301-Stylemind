package com.stylemind.order.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectOrderCancellationRequest {
    @Size(max = 1000)
    private String rejectionReason;
}
