package com.stylemind.payment.dto;

import lombok.*;

import java.math.BigDecimal;

// Mirrors SePay's documented "Data will be sent to Webhook URL" payload shape
// (https://docs.sepay.vn) - a bank-transfer notification, not a card/redirect
// callback. `id` is SePay's own transaction id (our idempotency key); `content`
// is the raw bank transfer note we reconcile against Transaction.transferContent;
// `transferAmount` is the exact VND amount received.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SepayWebhookPayload {
    private Long id;
    private String gateway;
    private String transactionDate;
    private String accountNumber;
    private String code;
    private String content;
    private String transferType; // "in" | "out"
    private BigDecimal transferAmount;
    private BigDecimal accumulated;
    private String referenceCode;
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }
    public String getTransactionDate() { return transactionDate; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTransferType() { return transferType; }
    public void setTransferType(String transferType) { this.transferType = transferType; }
    public BigDecimal getTransferAmount() { return transferAmount; }
    public void setTransferAmount(BigDecimal transferAmount) { this.transferAmount = transferAmount; }
    public BigDecimal getAccumulated() { return accumulated; }
    public void setAccumulated(BigDecimal accumulated) { this.accumulated = accumulated; }
    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
