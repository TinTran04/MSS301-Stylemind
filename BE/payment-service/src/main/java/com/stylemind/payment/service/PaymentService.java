package com.stylemind.payment.service;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import com.stylemind.payment.dto.CodCheckoutRequest;
import com.stylemind.payment.dto.PaymentResponse;
import com.stylemind.payment.dto.SepayCheckoutRequest;
import com.stylemind.payment.dto.SepayWebhookPayload;
import com.stylemind.payment.entity.PaymentWebhookEvent;
import com.stylemind.payment.entity.Transaction;
import com.stylemind.payment.feign.OrderClient;
import com.stylemind.payment.repository.PaymentWebhookEventRepository;
import com.stylemind.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private static final String TRANSFER_CONTENT_PREFIX = "STYLEMIND ORD";
    private static final int NOTIFY_MAX_ATTEMPTS = 3;

    private final TransactionRepository transactionRepository;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final OrderClient orderClient;

    @Value("${app.vietqr.bank-id:970436}")
    private String vietQrBankId;

    @Value("${app.vietqr.account-no:0123456789}")
    private String vietQrAccountNo;

    @Value("${app.vietqr.account-name:STYLEMIND SANDBOX}")
    private String vietQrAccountName;

    @Value("${app.vietqr.expiry-minutes:15}")
    private long vietQrExpiryMinutes;

    // SePay's static per-integration webhook API key - never hardcode a real value;
    // this default is a dev/sandbox placeholder only (see BE/.env.example).
    @Value("${app.sepay.webhook-api-key:sepay-sandbox-webhook-key-2026}")
    private String sepayWebhookApiKey;

    public PaymentResponse createCodPayment(CodCheckoutRequest request) {
        Transaction transaction = Transaction.builder()
                .id(StringUtil.generateUniqueId())
                .orderId(request.getOrderId())
                .userId("")
                .amount(request.getAmount())
                .method("cod")
                .status("PENDING") // collected on delivery - out of band, not a gateway flow
                .transactionRef(StringUtil.generateUniqueId())
                .build();

        transaction = transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    public PaymentResponse createSepayPayment(SepayCheckoutRequest request) {
        String transferContent = buildTransferContent(request.getOrderId());

        Transaction transaction = Transaction.builder()
                .id(StringUtil.generateUniqueId())
                .orderId(request.getOrderId())
                .userId("")
                .amount(request.getAmount())
                .method("sepay")
                .status("PENDING")
                .transactionRef(StringUtil.generateUniqueId())
                .transferContent(transferContent)
                .build();

        transaction = transactionRepository.save(transaction);

        return PaymentResponse.builder()
                .transactionId(transaction.getId())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .transferContent(transferContent)
                .qrContent(buildQrContent(transaction.getAmount(), transferContent))
                .qrImageUrl(buildQrImageUrl(transaction.getAmount(), transferContent))
                .expiresAt(Instant.now().plus(vietQrExpiryMinutes, ChronoUnit.MINUTES))
                .build();
    }

    public PaymentResponse getPaymentStatus(String orderId) {
        Transaction transaction = transactionRepository.findByOrderId(orderId).stream()
                .max(Comparator.comparing(Transaction::getCreatedAt))
                .orElseThrow(() -> new BusinessException(
                        "TRANSACTION_NOT_FOUND", "No transaction found for order: " + orderId, 404));
        return toResponse(transaction);
    }

    // Entry point for SePay's webhook. Order of checks matters: authenticity first
    // (§SEC-06), then idempotency (never apply the same gateway transaction twice),
    // then reconciliation (content AND amount must both match before we call it PAID).
    public void processSepayWebhook(String authorizationHeader, SepayWebhookPayload payload) {
        if (!isAuthorized(authorizationHeader)) {
            log.warn("Rejected SePay webhook: missing/invalid credentials (gatewayTxnId={})", payload.getId());
            logWebhookEvent(gatewayTransactionId(payload), null, payload.getContent(), payload.getTransferAmount(), "INVALID_SIGNATURE");
            throw new BusinessException("WEBHOOK_UNAUTHORIZED", "Invalid webhook credentials", 401);
        }

        String gatewayTransactionId = gatewayTransactionId(payload);
        if (gatewayTransactionId == null) {
            logWebhookEvent(null, null, payload.getContent(), payload.getTransferAmount(), "INVALID_PAYLOAD");
            throw new BusinessException("INVALID_WEBHOOK_PAYLOAD", "Webhook payload is missing an id", 400);
        }

        if (webhookEventRepository.findByGatewayTransactionId(gatewayTransactionId).isPresent()) {
            log.info("Ignoring duplicate SePay webhook delivery for gateway transaction {}", gatewayTransactionId);
            return;
        }

        if (!"in".equalsIgnoreCase(payload.getTransferType())) {
            logWebhookEvent(gatewayTransactionId, null, payload.getContent(), payload.getTransferAmount(), "IGNORED_OUTBOUND");
            return;
        }

        Transaction match = findPendingSepayTransactionByContent(payload.getContent());
        if (match == null) {
            logWebhookEvent(gatewayTransactionId, null, payload.getContent(), payload.getTransferAmount(), "NO_MATCHING_ORDER");
            log.warn("SePay webhook matched no pending order (gatewayTxnId={})", gatewayTransactionId);
            return;
        }

        boolean amountMatches = match.getAmount().compareTo(payload.getTransferAmount()) == 0;
        match.setGatewayTransactionId(gatewayTransactionId);

        if (!amountMatches) {
            match.setStatus("FAILED");
            transactionRepository.save(match);
            logWebhookEvent(gatewayTransactionId, match.getId(), payload.getContent(), payload.getTransferAmount(), "AMOUNT_MISMATCH");
            notifyOrderBestEffort(match.getOrderId(), "FAILED");
            return;
        }

        match.setStatus("COMPLETED");
        transactionRepository.save(match);
        logWebhookEvent(gatewayTransactionId, match.getId(), payload.getContent(), payload.getTransferAmount(), "MATCHED");
        notifyOrderBestEffort(match.getOrderId(), "PAID");
    }

    public void refund(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException("TRANSACTION_NOT_FOUND", "Transaction not found", 404));

        if (!"COMPLETED".equals(transaction.getStatus())) {
            throw new BusinessException("INVALID_REFUND", "Only completed transactions can be refunded", 400);
        }

        transaction.setStatus("REFUNDED");
        transactionRepository.save(transaction);
    }

    private String buildTransferContent(String orderId) {
        return TRANSFER_CONTENT_PREFIX + orderId;
    }

    private Transaction findPendingSepayTransactionByContent(String rawContent) {
        String normalizedWebhookContent = normalize(rawContent);
        return transactionRepository.findByMethodAndStatus("sepay", "PENDING").stream()
                .filter(t -> t.getTransferContent() != null
                        && normalizedWebhookContent.contains(normalize(t.getTransferContent())))
                .findFirst()
                .orElse(null);
    }

    // Banking apps routinely mangle whitespace/case in the transfer note, so
    // reconciliation compares a normalized form rather than an exact string match.
    private String normalize(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private String gatewayTransactionId(SepayWebhookPayload payload) {
        return payload.getId() == null ? null : String.valueOf(payload.getId());
    }

    // Constant-time comparison (MessageDigest.isEqual) so webhook auth doesn't leak
    // timing information about how much of the configured key was guessed correctly.
    private boolean isAuthorized(String authorizationHeader) {
        if (authorizationHeader == null) {
            return false;
        }
        String expected = "Apikey " + sepayWebhookApiKey;
        return MessageDigest.isEqual(
                authorizationHeader.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    private void logWebhookEvent(
            String gatewayTransactionId, String transactionId, String rawContent, BigDecimal amount, String result) {
        webhookEventRepository.save(PaymentWebhookEvent.builder()
                .id(StringUtil.generateUniqueId())
                .gatewayTransactionId(gatewayTransactionId)
                .transactionId(transactionId)
                .transferContent(rawContent)
                .amount(amount)
                .result(result)
                .build());
    }

    // Compensation guardrail mirroring order-service's own best-effort notifier:
    // the transaction's PAID/FAILED status is already durably persisted above, so
    // a failure to reach order-service here must never surface as a webhook error
    // (SePay would just retry the whole webhook, redelivering a payment that was
    // already reconciled).
    private void notifyOrderBestEffort(String orderId, String status) {
        for (int attempt = 1; attempt <= NOTIFY_MAX_ATTEMPTS; attempt++) {
            try {
                orderClient.updatePaymentStatus(
                        orderId, OrderClient.PaymentStatusUpdateRequest.builder().status(status).build());
                return;
            } catch (Exception ex) {
                if (attempt == NOTIFY_MAX_ATTEMPTS) {
                    log.warn("Failed to notify order-service of payment status {} for order {} after {} attempts: {}",
                            status, orderId, attempt, ex.getMessage());
                } else {
                    log.debug("Attempt {} to notify order-service failed for order {}, retrying: {}",
                            attempt, orderId, ex.getMessage());
                }
            }
        }
    }

    private String buildQrImageUrl(BigDecimal amount, String addInfo) {
        return String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%s&addInfo=%s&accountName=%s",
                vietQrBankId,
                vietQrAccountNo,
                amount.toBigInteger(),
                URLEncoder.encode(addInfo, StandardCharsets.UTF_8),
                URLEncoder.encode(vietQrAccountName, StandardCharsets.UTF_8));
    }

    private String buildQrContent(BigDecimal amount, String addInfo) {
        return String.format("%s|%s|%s|%s", vietQrBankId, vietQrAccountNo, amount.toBigInteger(), addInfo);
    }

    private PaymentResponse toResponse(Transaction transaction) {
        return PaymentResponse.builder()
                .transactionId(transaction.getId())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .transferContent(transaction.getTransferContent())
                .build();
    }
}
