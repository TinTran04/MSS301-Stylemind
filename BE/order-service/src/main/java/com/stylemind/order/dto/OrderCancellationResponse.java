package com.stylemind.order.dto;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCancellationResponse {
    private String id;
    private String orderId;
    private String userId;
    private String cancellationType;
    private String status;
    private String reasonCode;
    private String customerNote;
    private String adminNote;
    private String rejectionReason;
    private String requestedBy;
    private String reviewedBy;
    private Instant requestedAt;
    private Instant reviewedAt;
    private Instant approvedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
