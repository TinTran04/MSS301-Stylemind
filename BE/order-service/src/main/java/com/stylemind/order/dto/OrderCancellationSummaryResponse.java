package com.stylemind.order.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCancellationSummaryResponse {
    private long pendingCount;
}
