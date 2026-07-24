package com.stylemind.payment.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutDestinationResponse {
    private String returnRequestId;
    private String bankCode;
    private String accountHolder;
    private String maskedAccountNumber;
    private String status; // PROVIDED, MISSING
    private boolean editable;
    private LocalDateTime updatedAt;

    private String refundId;
    private java.math.BigDecimal amount;
    private String refundStatus;

    private String providerReference;
    private String proofUrl;
    private String note;
    private LocalDateTime processedAt;

    public String getReturnRequestId() { return returnRequestId; }
    public void setReturnRequestId(String returnRequestId) { this.returnRequestId = returnRequestId; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }
    public String getMaskedAccountNumber() { return maskedAccountNumber; }
    public void setMaskedAccountNumber(String maskedAccountNumber) { this.maskedAccountNumber = maskedAccountNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) { this.editable = editable; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getRefundId() { return refundId; }
    public void setRefundId(String refundId) { this.refundId = refundId; }
    public java.math.BigDecimal getAmount() { return amount; }
    public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
    public String getRefundStatus() { return refundStatus; }
    public void setRefundStatus(String refundStatus) { this.refundStatus = refundStatus; }
    public String getProviderReference() { return providerReference; }
    public void setProviderReference(String providerReference) { this.providerReference = providerReference; }
    public String getProofUrl() { return proofUrl; }
    public void setProofUrl(String proofUrl) { this.proofUrl = proofUrl; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public static PayoutDestinationResponseBuilder builder() { return new PayoutDestinationResponseBuilder(); }

    public static class PayoutDestinationResponseBuilder {
        private String returnRequestId;
        private String bankCode;
        private String accountHolder;
        private String maskedAccountNumber;
        private String status;
        private boolean editable;
        private LocalDateTime updatedAt;
        private String refundId;
        private java.math.BigDecimal amount;
        private String refundStatus;
        private String providerReference;
        private String proofUrl;
        private String note;
        private LocalDateTime processedAt;

        public PayoutDestinationResponseBuilder returnRequestId(String returnRequestId) { this.returnRequestId = returnRequestId; return this; }
        public PayoutDestinationResponseBuilder bankCode(String bankCode) { this.bankCode = bankCode; return this; }
        public PayoutDestinationResponseBuilder accountHolder(String accountHolder) { this.accountHolder = accountHolder; return this; }
        public PayoutDestinationResponseBuilder maskedAccountNumber(String maskedAccountNumber) { this.maskedAccountNumber = maskedAccountNumber; return this; }
        public PayoutDestinationResponseBuilder status(String status) { this.status = status; return this; }
        public PayoutDestinationResponseBuilder editable(boolean editable) { this.editable = editable; return this; }
        public PayoutDestinationResponseBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public PayoutDestinationResponseBuilder refundId(String refundId) { this.refundId = refundId; return this; }
        public PayoutDestinationResponseBuilder amount(java.math.BigDecimal amount) { this.amount = amount; return this; }
        public PayoutDestinationResponseBuilder refundStatus(String refundStatus) { this.refundStatus = refundStatus; return this; }
        public PayoutDestinationResponseBuilder providerReference(String providerReference) { this.providerReference = providerReference; return this; }
        public PayoutDestinationResponseBuilder proofUrl(String proofUrl) { this.proofUrl = proofUrl; return this; }
        public PayoutDestinationResponseBuilder note(String note) { this.note = note; return this; }
        public PayoutDestinationResponseBuilder processedAt(LocalDateTime processedAt) { this.processedAt = processedAt; return this; }

        public PayoutDestinationResponse build() {
            PayoutDestinationResponse r = new PayoutDestinationResponse();
            r.setReturnRequestId(this.returnRequestId);
            r.setBankCode(this.bankCode);
            r.setAccountHolder(this.accountHolder);
            r.setMaskedAccountNumber(this.maskedAccountNumber);
            r.setStatus(this.status);
            r.setEditable(this.editable);
            r.setUpdatedAt(this.updatedAt);
            r.setRefundId(this.refundId);
            r.setAmount(this.amount);
            r.setRefundStatus(this.refundStatus);
            r.setProviderReference(this.providerReference);
            r.setProofUrl(this.proofUrl);
            r.setNote(this.note);
            r.setProcessedAt(this.processedAt);
            return r;
        }
    }
}
