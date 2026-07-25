package com.stylemind.payment.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund_transactions")
@Getter
@Setter
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

    @Column(name = "order_cancellation_id", length = 50)
    private String orderCancellationId;

    @Column(name = "return_request_id", length = 64)
    private String returnRequestId;

    @Column(name = "bank_code", length = 32)
    private String bankCode;

    @Column(name = "account_holder", length = 150)
    private String accountHolder;

    @Column(name = "account_number", length = 64)
    private String accountNumber;

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

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getPaymentTransactionId() { return paymentTransactionId; }
    public void setPaymentTransactionId(String paymentTransactionId) { this.paymentTransactionId = paymentTransactionId; }
    public String getOrderCancellationId() { return orderCancellationId; }
    public void setOrderCancellationId(String orderCancellationId) { this.orderCancellationId = orderCancellationId; }
    public String getReturnRequestId() { return returnRequestId; }
    public void setReturnRequestId(String returnRequestId) { this.returnRequestId = returnRequestId; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public RefundStatus getStatus() { return status; }
    public void setStatus(RefundStatus status) { this.status = status; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getProviderReference() { return providerReference; }
    public void setProviderReference(String providerReference) { this.providerReference = providerReference; }
    public String getProofUrl() { return proofUrl; }
    public void setProofUrl(String proofUrl) { this.proofUrl = proofUrl; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public LocalDateTime getCreatedAt() { return super.getCreatedAt(); }
    public LocalDateTime getUpdatedAt() { return super.getUpdatedAt(); }

    public static RefundTransactionBuilder builder() { return new RefundTransactionBuilder(); }

    public static class RefundTransactionBuilder {
        private String id;
        private String orderId;
        private String paymentTransactionId;
        private String orderCancellationId;
        private String returnRequestId;
        private String bankCode;
        private String accountHolder;
        private String accountNumber;
        private BigDecimal amount;
        private RefundStatus status;
        private String method;
        private String providerReference;
        private String proofUrl;
        private String note;
        private String processedBy;
        private LocalDateTime processedAt;
        private String failureReason;

        public RefundTransactionBuilder id(String id) { this.id = id; return this; }
        public RefundTransactionBuilder orderId(String orderId) { this.orderId = orderId; return this; }
        public RefundTransactionBuilder paymentTransactionId(String paymentTransactionId) { this.paymentTransactionId = paymentTransactionId; return this; }
        public RefundTransactionBuilder orderCancellationId(String orderCancellationId) { this.orderCancellationId = orderCancellationId; return this; }
        public RefundTransactionBuilder returnRequestId(String returnRequestId) { this.returnRequestId = returnRequestId; return this; }
        public RefundTransactionBuilder bankCode(String bankCode) { this.bankCode = bankCode; return this; }
        public RefundTransactionBuilder accountHolder(String accountHolder) { this.accountHolder = accountHolder; return this; }
        public RefundTransactionBuilder accountNumber(String accountNumber) { this.accountNumber = accountNumber; return this; }
        public RefundTransactionBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public RefundTransactionBuilder status(RefundStatus status) { this.status = status; return this; }
        public RefundTransactionBuilder method(String method) { this.method = method; return this; }
        public RefundTransactionBuilder providerReference(String providerReference) { this.providerReference = providerReference; return this; }
        public RefundTransactionBuilder proofUrl(String proofUrl) { this.proofUrl = proofUrl; return this; }
        public RefundTransactionBuilder note(String note) { this.note = note; return this; }
        public RefundTransactionBuilder processedBy(String processedBy) { this.processedBy = processedBy; return this; }
        public RefundTransactionBuilder processedAt(LocalDateTime processedAt) { this.processedAt = processedAt; return this; }
        public RefundTransactionBuilder failureReason(String failureReason) { this.failureReason = failureReason; return this; }

        public RefundTransaction build() {
            RefundTransaction r = new RefundTransaction();
            r.setId(this.id);
            r.setOrderId(this.orderId);
            r.setPaymentTransactionId(this.paymentTransactionId);
            r.setOrderCancellationId(this.orderCancellationId);
            r.setReturnRequestId(this.returnRequestId);
            r.setBankCode(this.bankCode);
            r.setAccountHolder(this.accountHolder);
            r.setAccountNumber(this.accountNumber);
            r.setAmount(this.amount);
            r.setStatus(this.status);
            r.setMethod(this.method);
            r.setProviderReference(this.providerReference);
            r.setProofUrl(this.proofUrl);
            r.setNote(this.note);
            r.setProcessedBy(this.processedBy);
            r.setProcessedAt(this.processedAt);
            r.setFailureReason(this.failureReason);
            return r;
        }
    }
}
