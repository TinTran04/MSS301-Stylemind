package com.stylemind.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistoryResponse {
    private String id;
    private String previousStatus;
    private String newStatus;
    private String actor;
    private Instant timestamp;
}
