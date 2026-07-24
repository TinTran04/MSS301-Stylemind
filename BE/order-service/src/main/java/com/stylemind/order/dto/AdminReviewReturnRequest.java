package com.stylemind.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReviewReturnRequest {

    @NotNull(message = "ACTION_REQUIRED")
    private String action; // APPROVE, REJECT

    private Boolean isPhysicalReturn; // true (customer ships back), false (no-return refund)

    private String adminNote;

    private String rejectionReason;
}
