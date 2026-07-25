package com.stylemind.payment.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Version
    @Column(name = "version")
    private Long version;

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

    @Column(name = "order_cancellation_id", length = 50)
    private String orderCancellationId;

    // Explicit getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
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
    public String getOrderCancellationId() { return orderCancellationId; }
    public void setOrderCancellationId(String orderCancellationId) { this.orderCancellationId = orderCancellationId; }

    public static TransactionBuilder builder() { return new TransactionBuilder(); }

    public static class TransactionBuilder {
        private String id;
        private Long version;
        private String orderId;
        private String userId;
        private java.math.BigDecimal amount;
        private String method;
        private String status = "PENDING";
        private String transactionRef;
        private String transferContent;
        private String gatewayTransactionId;
        private java.time.LocalDateTime expiresAt;
        private java.time.LocalDateTime paidAt;
        private String orderCancellationId;

        public TransactionBuilder id(String id) { this.id = id; return this; }
        public TransactionBuilder version(Long version) { this.version = version; return this; }
        public TransactionBuilder orderId(String orderId) { this.orderId = orderId; return this; }
        public TransactionBuilder userId(String userId) { this.userId = userId; return this; }
        public TransactionBuilder amount(java.math.BigDecimal amount) { this.amount = amount; return this; }
        public TransactionBuilder method(String method) { this.method = method; return this; }
        public TransactionBuilder status(String status) { this.status = status; return this; }
        public TransactionBuilder transactionRef(String transactionRef) { this.transactionRef = transactionRef; return this; }
        public TransactionBuilder transferContent(String transferContent) { this.transferContent = transferContent; return this; }
        public TransactionBuilder gatewayTransactionId(String gatewayTransactionId) { this.gatewayTransactionId = gatewayTransactionId; return this; }
        public TransactionBuilder expiresAt(java.time.LocalDateTime expiresAt) { this.expiresAt = expiresAt; return this; }
        public TransactionBuilder paidAt(java.time.LocalDateTime paidAt) { this.paidAt = paidAt; return this; }
        public TransactionBuilder orderCancellationId(String orderCancellationId) { this.orderCancellationId = orderCancellationId; return this; }

        public Transaction build() {
            Transaction t = new Transaction();
            t.setId(this.id);
            t.setVersion(this.version);
            t.setOrderId(this.orderId);
            t.setUserId(this.userId);
            t.setAmount(this.amount);
            t.setMethod(this.method);
            if (this.status != null) t.setStatus(this.status);
            t.setTransactionRef(this.transactionRef);
            t.setTransferContent(this.transferContent);
            t.setGatewayTransactionId(this.gatewayTransactionId);
            t.setExpiresAt(this.expiresAt);
            t.setPaidAt(this.paidAt);
            t.setOrderCancellationId(this.orderCancellationId);
            return t;
        }
    }
}
