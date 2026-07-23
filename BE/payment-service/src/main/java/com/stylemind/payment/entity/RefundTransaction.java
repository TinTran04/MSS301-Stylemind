package com.stylemind.payment.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund_transactions")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundTransaction extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "order_id", length = 50, nullable = false)
    private String orderId;

    @Column(name = "payment_transaction_id", length = 50, nullable = false)
    private String paymentTransactionId;

    @Column(name = "order_cancellation_id", length = 50, nullable = false, unique = true)
    private String orderCancellationId;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private RefundStatus status;

    @Column(name = "method", length = 30, nullable = false)
    private String method;

    @Column(name = "provider_reference", length = 150)
    private String providerReference;

    @Column(name = "proof_url", columnDefinition = "TEXT")
    private String proofUrl;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "processed_by", length = 50)
    private String processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
}
