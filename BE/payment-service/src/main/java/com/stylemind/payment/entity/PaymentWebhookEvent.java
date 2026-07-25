package com.stylemind.payment.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

// One row per webhook delivery attempt (SePay retries on non-2xx, and the same
// gatewayTransactionId can arrive more than once) - the audit trail the spec
// requires, and the source of truth for the idempotency check. Never stores the
// webhook's Authorization/API-key header.
@Entity
@Table(name = "payment_webhook_events")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentWebhookEvent extends BaseEntity {

    public static final String PROVIDER_SEPAY = "SEPAY";

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "provider", length = 30, nullable = false)
    private String provider;

    @Column(name = "gateway_transaction_id", length = 100)
    private String gatewayTransactionId;

    @Column(name = "transaction_id", length = 50)
    private String transactionId;

    @Column(name = "transfer_content", length = 200)
    private String transferContent;

    @Column(name = "amount", precision = 12, scale = 2)
    private java.math.BigDecimal amount;

    @Column(name = "result", length = 30, nullable = false)
    private String result;

    @Column(name = "processed", nullable = false)
    @Builder.Default
    private Boolean processed = Boolean.FALSE;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getGatewayTransactionId() { return gatewayTransactionId; }
    public void setGatewayTransactionId(String gatewayTransactionId) { this.gatewayTransactionId = gatewayTransactionId; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getTransferContent() { return transferContent; }
    public void setTransferContent(String transferContent) { this.transferContent = transferContent; }
    public java.math.BigDecimal getAmount() { return amount; }
    public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public Boolean getProcessed() { return processed; }
    public void setProcessed(Boolean processed) { this.processed = processed; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
