package com.stylemind.order.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnResponse {
    private String id;
    private String orderId;
    private String userId;
    private String status;
    private String reason;
    private String customerNote;
    private String adminNote;
    private String rejectionReason;
    private Boolean isPhysicalReturn;
    private String payoutState;
    private String refundId;
    private String reviewedBy;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime qcCompletedAt;
    private LocalDateTime closedAt;

    private List<ReturnItemResponse> items;
    private List<ReturnEvidenceResponse> evidences;
    private ReturnShipmentResponse shipment;
}
