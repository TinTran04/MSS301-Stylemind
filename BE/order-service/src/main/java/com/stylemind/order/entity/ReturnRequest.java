package com.stylemind.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "return_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRequest {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReturnStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReturnReason reason;

    @Column(columnDefinition = "TEXT")
    private String customerNote;

    @Column(columnDefinition = "TEXT")
    private String adminNote;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "is_physical_return")
    private Boolean isPhysicalReturn;

    @Column(name = "payout_state", length = 32)
    private String payoutState; // NOT_REQUIRED, MISSING, PROVIDED

    @Column(name = "refund_id", length = 64)
    private String refundId;

    @Column(name = "reviewed_by", length = 64)
    private String reviewedBy;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "qc_completed_at")
    private LocalDateTime qcCompletedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReturnItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReturnEvidence> evidences = new ArrayList<>();

    @OneToOne(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private ReturnShipment shipment;

    public void addItem(ReturnItem item) {
        items.add(item);
        item.setReturnRequest(this);
    }

    public void addEvidence(ReturnEvidence evidence) {
        evidences.add(evidence);
        evidence.setReturnRequest(this);
    }

    public void setShipment(ReturnShipment shipment) {
        this.shipment = shipment;
        if (shipment != null) {
            shipment.setReturnRequest(this);
        }
    }
}
