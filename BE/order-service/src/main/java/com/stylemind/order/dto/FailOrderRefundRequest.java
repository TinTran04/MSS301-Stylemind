package com.stylemind.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailOrderRefundRequest {
    @NotBlank
    private String failureReason;
}
