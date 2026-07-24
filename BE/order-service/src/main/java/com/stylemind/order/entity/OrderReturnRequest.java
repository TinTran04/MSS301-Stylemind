package com.stylemind.order.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_return_requests")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReturnRequest extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "order_id", length = 50, nullable = false, unique = true)
    private String orderId;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 40, nullable = false)
    private OrderReturnStatus status;

    @Column(name = "reason_code", length = 80, nullable = false)
    private String reasonCode;

    @Column(name = "customer_note", columnDefinition = "TEXT")
    private String customerNote;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "bank_name", length = 120)
    private String bankName;

    @Column(name = "bank_account_number", length = 80)
    private String bankAccountNumber;

    @Column(name = "bank_account_holder", length = 150)
    private String bankAccountHolder;

    @Column(name = "bank_branch", length = 150)
    private String bankBranch;

    @Column(name = "refund_reference", length = 150)
    private String refundReference;

    @Column(name = "refund_note", columnDefinition = "TEXT")
    private String refundNote;

    @Column(name = "requested_by", length = 50, nullable = false)
    private String requestedBy;

    @Column(name = "reviewed_by", length = 50)
    private String reviewedBy;

    @Column(name = "processed_by", length = 50)
    private String processedBy;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "bank_info_submitted_at")
    private LocalDateTime bankInfoSubmittedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;
}
