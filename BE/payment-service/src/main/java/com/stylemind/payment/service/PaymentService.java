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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private static final String PROVIDER_SEPAY = "SEPAY";
    private static final String METHOD_COD = "cod";
    private static final String METHOD_SEPAY = "sepay";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final int NOTIFY_MAX_ATTEMPTS = 3;

    private final TransactionRepository transactionRepository;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final OrderClient orderClient;
    private final PaymentReferenceMatcher paymentReferenceMatcher;

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
                .userId(request.getUserId())
                .amount(request.getAmount())
                .method(METHOD_COD)
                .status(STATUS_PENDING) // collected on delivery - out of band, not a gateway flow
                .transactionRef(StringUtil.generateUniqueId())
                .build();

        transaction = transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    public PaymentResponse createSepayPayment(SepayCheckoutRequest request) {
        String transactionId = StringUtil.generateUniqueId();
        String paymentToken = buildPaymentToken(transactionId);
        String transferContent = buildTransferContent(paymentToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(vietQrExpiryMinutes);

        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .method(METHOD_SEPAY)
                .status(STATUS_PENDING)
                .transactionRef(paymentToken)
                .transferContent(transferContent)
                .expiresAt(expiresAt)
                .build();

        transaction = transactionRepository.save(transaction);

        return PaymentResponse.builder()
                .transactionId(transaction.getId())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .transferContent(transferContent)
                .qrContent(buildQrContent(transaction.getAmount(), transferContent))
                .qrImageUrl(buildQrImageUrl(transaction.getAmount(), transferContent))
                .expiresAt(expiresAt.atZone(java.time.ZoneId.systemDefault()).toInstant())
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
            logWebhookEvent(gatewayTransactionId(payload), null, rawTransferContent(payload), payload.getTransferAmount(), "INVALID_SIGNATURE", false, null);
            throw new BusinessException("WEBHOOK_UNAUTHORIZED", "Invalid webhook credentials", 401);
        }

        String gatewayTransactionId = gatewayTransactionId(payload);
        if (gatewayTransactionId == null) {
            logWebhookEvent(null, null, rawTransferContent(payload), payload.getTransferAmount(), "INVALID_PAYLOAD", false, null);
            throw new BusinessException("INVALID_WEBHOOK_PAYLOAD", "Webhook payload is missing an id", 400);
        }

        if (webhookEventRepository.findByProviderAndGatewayTransactionId(PROVIDER_SEPAY, gatewayTransactionId).isPresent()) {
            log.info("Ignoring duplicate SePay webhook delivery for gateway transaction {}", gatewayTransactionId);
            return;
        }

        PaymentWebhookEvent webhookEvent = createWebhookEvent(payload, gatewayTransactionId);
        if (webhookEvent == null) {
            log.info("Ignoring duplicate SePay webhook delivery for gateway transaction {}", gatewayTransactionId);
            return;
        }

        if (!"in".equalsIgnoreCase(payload.getTransferType())) {
            finalizeWebhookEvent(webhookEvent, null, "IGNORED_OUTBOUND", true, null);
            return;
        }

        String incomingTransferContent = rawTransferContent(payload);
        Transaction match = findPendingSepayTransactionByContent(incomingTransferContent);
        if (match == null) {
            Transaction expiredMatch = findExpiredSepayTransactionByContent(incomingTransferContent);
            if (expiredMatch != null) {
                finalizeWebhookEvent(webhookEvent, expiredMatch.getId(), "LATE_AFTER_EXPIRY", true,
                        "Webhook arrived after payment/order expiration");
                log.warn("Late SePay webhook arrived for expired payment orderId={} gatewayTxnId={}",
                        expiredMatch.getOrderId(), gatewayTransactionId);
                return;
            }
            finalizeWebhookEvent(webhookEvent, null, "NO_MATCHING_ORDER", true, null);
            log.warn("SePay webhook matched no pending order (gatewayTxnId={})", gatewayTransactionId);
            return;
        }

        boolean amountMatches = match.getAmount().compareTo(payload.getTransferAmount()) == 0;

        if (!amountMatches) {
            match.setStatus(STATUS_FAILED);
            match.setGatewayTransactionId(gatewayTransactionId);
            transactionRepository.save(match);
            finalizeWebhookEvent(webhookEvent, match.getId(), "AMOUNT_MISMATCH", true, "Transfer amount does not match expected amount");
            notifyOrderBestEffort(match.getOrderId(), "FAILED");
            return;
        }

        match.setStatus(STATUS_PAID);
        match.setGatewayTransactionId(gatewayTransactionId);
        match.setPaidAt(LocalDateTime.now());
        transactionRepository.save(match);
        finalizeWebhookEvent(webhookEvent, match.getId(), "MATCHED", true, null);
        notifyOrderBestEffort(match.getOrderId(), "PAID");
    }

    public void expirePendingSepayPayment(String orderId) {
        Transaction transaction = transactionRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new BusinessException(
                        "TRANSACTION_NOT_FOUND", "No transaction found for order: " + orderId, 404));

        if (!METHOD_SEPAY.equalsIgnoreCase(transaction.getMethod())) {
            return;
        }

        if (STATUS_PAID.equalsIgnoreCase(transaction.getStatus())) {
            log.info("Ignoring expire request for already-paid SePay transaction {} on order {}", transaction.getId(), orderId);
            return;
        }

        if (STATUS_EXPIRED.equalsIgnoreCase(transaction.getStatus()) || "CANCELLED".equalsIgnoreCase(transaction.getStatus())) {
            return;
        }

        if (STATUS_PENDING.equalsIgnoreCase(transaction.getStatus())) {
            transaction.setStatus(STATUS_EXPIRED);
            transactionRepository.save(transaction);
        }
    }

    public void refund(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException("TRANSACTION_NOT_FOUND", "Transaction not found", 404));

        if (!STATUS_PAID.equals(transaction.getStatus())) {
            throw new BusinessException("INVALID_REFUND", "Only completed transactions can be refunded", 400);
        }

        transaction.setStatus("REFUNDED");
        transactionRepository.save(transaction);
    }

    private String buildPaymentToken(String transactionId) {
        String compactId = transactionId == null ? StringUtil.generateShortId() : transactionId;
        return ("SM" + compactId.substring(0, Math.min(compactId.length(), 10))).toUpperCase();
    }

    private String buildTransferContent(String paymentToken) {
        return "STYLEMIND " + paymentToken;
    }

    private Transaction findPendingSepayTransactionByContent(String rawContent) {
        return transactionRepository.findByMethodAndStatus(METHOD_SEPAY, STATUS_PENDING).stream()
                .filter(t -> paymentReferenceMatcher.matches(t.getTransferContent(), rawContent))
                .findFirst()
                .orElse(null);
    }

    private Transaction findExpiredSepayTransactionByContent(String rawContent) {
        return transactionRepository.findByMethodAndStatusIn(METHOD_SEPAY, List.of(STATUS_EXPIRED, STATUS_FAILED, "CANCELLED")).stream()
                .filter(t -> paymentReferenceMatcher.matches(t.getTransferContent(), rawContent))
                .findFirst()
                .orElse(null);
    }

    private String gatewayTransactionId(SepayWebhookPayload payload) {
        return payload.getId() == null ? null : String.valueOf(payload.getId());
    }

    private String rawTransferContent(SepayWebhookPayload payload) {
        if (payload == null) {
            return "";
        }
        if (payload.getContent() != null && !payload.getContent().isBlank()) {
            return payload.getContent();
        }
        if (payload.getDescription() != null && !payload.getDescription().isBlank()) {
            return payload.getDescription();
        }
        if (payload.getReferenceCode() != null && !payload.getReferenceCode().isBlank()) {
            return payload.getReferenceCode();
        }
        return "";
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

    private PaymentWebhookEvent createWebhookEvent(SepayWebhookPayload payload, String gatewayTransactionId) {
        PaymentWebhookEvent webhookEvent = PaymentWebhookEvent.builder()
                .id(StringUtil.generateUniqueId())
                .provider(PROVIDER_SEPAY)
                .gatewayTransactionId(gatewayTransactionId)
                .transactionId(null)
                .transferContent(rawTransferContent(payload))
                .amount(payload.getTransferAmount())
                .result("RECEIVED")
                .processed(Boolean.FALSE)
                .build();
        try {
            return webhookEventRepository.save(webhookEvent);
        } catch (DataIntegrityViolationException ex) {
            return null;
        }
    }

    private void finalizeWebhookEvent(
            PaymentWebhookEvent webhookEvent,
            String transactionId,
            String result,
            boolean processed,
            String errorMessage) {
        webhookEvent.setTransactionId(transactionId);
        webhookEvent.setResult(result);
        webhookEvent.setProcessed(processed);
        webhookEvent.setErrorMessage(errorMessage);
        webhookEventRepository.save(webhookEvent);
    }

    private void logWebhookEvent(
            String gatewayTransactionId, String transactionId, String rawContent, BigDecimal amount, String result,
            boolean processed, String errorMessage) {
        webhookEventRepository.save(PaymentWebhookEvent.builder()
                .id(StringUtil.generateUniqueId())
                .provider(PROVIDER_SEPAY)
                .gatewayTransactionId(gatewayTransactionId)
                .transactionId(transactionId)
                .transferContent(rawContent)
                .amount(amount)
                .result(result)
                .processed(processed)
                .errorMessage(errorMessage)
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
                .expiresAt(transaction.getExpiresAt() == null
                        ? null
                        : transaction.getExpiresAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .build();
    }
}
