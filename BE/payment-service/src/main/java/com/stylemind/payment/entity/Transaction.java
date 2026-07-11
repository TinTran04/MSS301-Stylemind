package com.stylemind.payment.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transactions")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "order_id", length = 50, nullable = false)
    private String orderId;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private java.math.BigDecimal amount;

    @Column(name = "method", length = 30, nullable = false)
    private String method;

    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    // Unique per-order note the customer must transfer with (e.g. "SEVQR STYLEMIND SMabc123"),
    // used to reconcile an inbound SePay webhook to this transaction. Null for COD.
    @Column(name = "transfer_content", length = 100)
    private String transferContent;

    // SePay's own transaction id from the webhook payload ("id" field) - the idempotency
    // key that lets us detect and no-op a redelivered webhook. Null until a webhook matches.
    @Column(name = "gateway_transaction_id", length = 100, unique = true)
    private String gatewayTransactionId;

    @Column(name = "expires_at")
    private java.time.LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private java.time.LocalDateTime paidAt;

    // Explicit getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public java.math.BigDecimal getAmount() { return amount; }
    public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
    public String getTransferContent() { return transferContent; }
    public void setTransferContent(String transferContent) { this.transferContent = transferContent; }
    public String getGatewayTransactionId() { return gatewayTransactionId; }
    public void setGatewayTransactionId(String gatewayTransactionId) { this.gatewayTransactionId = gatewayTransactionId; }
    public java.time.LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(java.time.LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public java.time.LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(java.time.LocalDateTime paidAt) { this.paidAt = paidAt; }
}
