package com.stylemind.order.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReturnRequestResponse {
    private String id;
    private String orderId;
    private String userId;
    private String status;
    private String reasonCode;
    private String customerNote;
    private String adminNote;
    private String rejectionReason;
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountHolder;
    private String bankBranch;
    private String refundReference;
    private String refundNote;
    private String requestedBy;
    private String reviewedBy;
    private String processedBy;
    private Instant requestedAt;
    private Instant reviewedAt;
    private Instant approvedAt;
    private Instant bankInfoSubmittedAt;
    private Instant processedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private List<OrderReturnAttachmentResponse> attachments;
}
