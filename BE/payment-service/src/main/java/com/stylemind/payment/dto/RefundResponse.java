package com.stylemind.payment.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponse {
    private String id;
    private String orderId;
    private String paymentTransactionId;
    private String orderCancellationId;
    private String returnRequestId;
    private String bankCode;
    private String accountHolder;
    private String maskedAccountNumber;
    private BigDecimal amount;
    private String status;
    private String method;
    private String providerReference;
    private String proofUrl;
    private String note;
    private String processedBy;
    private Instant processedAt;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;

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
    public String getMaskedAccountNumber() { return maskedAccountNumber; }
    public void setMaskedAccountNumber(String maskedAccountNumber) { this.maskedAccountNumber = maskedAccountNumber; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
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
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static RefundResponseBuilder builder() { return new RefundResponseBuilder(); }

    public static class RefundResponseBuilder {
        private String id;
        private String orderId;
        private String paymentTransactionId;
        private String orderCancellationId;
        private String returnRequestId;
        private String bankCode;
        private String accountHolder;
        private String maskedAccountNumber;
        private BigDecimal amount;
        private String status;
        private String method;
        private String providerReference;
        private String proofUrl;
        private String note;
        private String processedBy;
        private Instant processedAt;
        private String failureReason;
        private Instant createdAt;
        private Instant updatedAt;

        public RefundResponseBuilder id(String id) { this.id = id; return this; }
        public RefundResponseBuilder orderId(String orderId) { this.orderId = orderId; return this; }
        public RefundResponseBuilder paymentTransactionId(String paymentTransactionId) { this.paymentTransactionId = paymentTransactionId; return this; }
        public RefundResponseBuilder orderCancellationId(String orderCancellationId) { this.orderCancellationId = orderCancellationId; return this; }
        public RefundResponseBuilder returnRequestId(String returnRequestId) { this.returnRequestId = returnRequestId; return this; }
        public RefundResponseBuilder bankCode(String bankCode) { this.bankCode = bankCode; return this; }
        public RefundResponseBuilder accountHolder(String accountHolder) { this.accountHolder = accountHolder; return this; }
        public RefundResponseBuilder maskedAccountNumber(String maskedAccountNumber) { this.maskedAccountNumber = maskedAccountNumber; return this; }
        public RefundResponseBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public RefundResponseBuilder status(String status) { this.status = status; return this; }
        public RefundResponseBuilder method(String method) { this.method = method; return this; }
        public RefundResponseBuilder providerReference(String providerReference) { this.providerReference = providerReference; return this; }
        public RefundResponseBuilder proofUrl(String proofUrl) { this.proofUrl = proofUrl; return this; }
        public RefundResponseBuilder note(String note) { this.note = note; return this; }
        public RefundResponseBuilder processedBy(String processedBy) { this.processedBy = processedBy; return this; }
        public RefundResponseBuilder processedAt(Instant processedAt) { this.processedAt = processedAt; return this; }
        public RefundResponseBuilder failureReason(String failureReason) { this.failureReason = failureReason; return this; }
        public RefundResponseBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public RefundResponseBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public RefundResponse build() {
            RefundResponse r = new RefundResponse();
            r.setId(this.id);
            r.setOrderId(this.orderId);
            r.setPaymentTransactionId(this.paymentTransactionId);
            r.setOrderCancellationId(this.orderCancellationId);
            r.setReturnRequestId(this.returnRequestId);
            r.setBankCode(this.bankCode);
            r.setAccountHolder(this.accountHolder);
            r.setMaskedAccountNumber(this.maskedAccountNumber);
            r.setAmount(this.amount);
            r.setStatus(this.status);
            r.setMethod(this.method);
            r.setProviderReference(this.providerReference);
            r.setProofUrl(this.proofUrl);
            r.setNote(this.note);
            r.setProcessedBy(this.processedBy);
            r.setProcessedAt(this.processedAt);
            r.setFailureReason(this.failureReason);
            r.setCreatedAt(this.createdAt);
            r.setUpdatedAt(this.updatedAt);
            return r;
        }
    }
}
