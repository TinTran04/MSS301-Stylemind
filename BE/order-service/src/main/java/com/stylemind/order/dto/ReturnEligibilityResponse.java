package com.stylemind.order.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnEligibilityResponse {
    private boolean eligible;
    private String reasonCode;
    private String message;
    private LocalDateTime completedAt;
    private LocalDateTime returnWindowEnd;
    private Map<String, Integer> remainingReturnableQuantities; // orderItemId -> remaining returnable qty
}
