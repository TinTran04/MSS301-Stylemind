package com.stylemind.payment.service.impl;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import com.stylemind.payment.dto.CodCheckoutRequest;
import com.stylemind.payment.dto.PaymentResponse;
import com.stylemind.payment.dto.PaymentRevenueCandidate;
import com.stylemind.payment.dto.SepayCheckoutRequest;
import com.stylemind.payment.dto.SepayWebhookPayload;
import com.stylemind.payment.entity.PaymentWebhookEvent;
import com.stylemind.payment.entity.Transaction;
import com.stylemind.payment.feign.OrderClient;
import com.stylemind.payment.repository.PaymentWebhookEventRepository;
import com.stylemind.payment.repository.TransactionRepository;
import com.stylemind.payment.service.PaymentReferenceMatcher;
import com.stylemind.payment.service.PaymentService;
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
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

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

    @Value("${app.vietqr.required-transfer-prefix:SEVQR}")
    private String bankHubTransferPrefix;

    @Value("${app.vietqr.expiry-minutes:15}")
    private long vietQrExpiryMinutes;

    @Value("${app.vietqr.qr-base-url:https://img.vietqr.io/image}")
    private String vietQrBaseUrl;

    @Value("${app.sepay.payment-code-prefix:STYLEMIND}")
    private String paymentCodePrefix;

    // SePay's static per-integration webhook API key must be supplied by the
    // environment. Never commit a fallback secret.
    @Value("${app.sepay.webhook-api-key}")
    private String sepayWebhookApiKey;

    @Override
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

    @Override
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

        return toResponse(transaction);
    }

    @Override
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
    @Override
    public void processSepayWebhook(String authorizationHeader, SepayWebhookPayload payload) {
        if (!isAuthorized(authorizationHeader)) {
            log.warn("Rejected SePay webhook: missing/invalid credentials (gatewayTxnId={})", payload.getId());
            logWebhookEvent(gatewayTransactionId(payload), null, rawTransferContent(payload), payload.getTransferAmount(), "INVALID_SIGNATURE", false, null);
            throw new BusinessException("WEBHOOK_UNAUTHORIZED", "Invalid webhook credentials", 401);
        }

        log.info("Authenticated SePay webhook accepted for gatewayTxnId={} fieldCount={}",
                payload.getId(), rawTransferContents(payload).size());

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

        List<String> incomingTransferContents = rawTransferContents(payload);
        String incomingTransferContent = String.join(" | ", incomingTransferContents);
        Transaction match = findPendingSepayTransactionByContent(incomingTransferContents);
        if (match == null) {
            Transaction expiredMatch = findExpiredSepayTransactionByContent(incomingTransferContents);
            if (expiredMatch != null) {
                finalizeWebhookEvent(webhookEvent, expiredMatch.getId(), "LATE_AFTER_EXPIRY", false,
                        "Webhook arrived after payment/order expiration");
                log.warn("Late SePay webhook arrived for expired payment orderId={} gatewayTxnId={}",
                        expiredMatch.getOrderId(), gatewayTransactionId);
                return;
            }
            finalizeWebhookEvent(webhookEvent, null, "NO_MATCHING_ORDER", false,
                    "No pending SePay transaction matched transfer_content");
            log.warn("SePay webhook matched no pending order (gatewayTxnId={})", gatewayTransactionId);
            return;
        }

        boolean amountMatches = payload.getTransferAmount() != null
                && match.getAmount().compareTo(payload.getTransferAmount()) == 0;

        if (!amountMatches) {
            match.setStatus(STATUS_FAILED);
            match.setGatewayTransactionId(gatewayTransactionId);
            transactionRepository.save(match);
            finalizeWebhookEvent(webhookEvent, match.getId(), "AMOUNT_MISMATCH", false, "Transfer amount does not match expected amount");
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

    @Override
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

    @Override
    public void refund(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException("TRANSACTION_NOT_FOUND", "Transaction not found", 404));

        if (!STATUS_PAID.equals(transaction.getStatus())) {
            throw new BusinessException("INVALID_REFUND", "Only completed transactions can be refunded", 400);
        }

        transaction.setStatus("REFUNDED");
        transactionRepository.save(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentRevenueCandidate> findSepayRevenueCandidates(LocalDateTime from, LocalDateTime to) {
        return transactionRepository.findSepayRevenueCandidates(from, to).stream()
                .map(this::toRevenueCandidate)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentRevenueCandidate> findRevenueCandidatesByOrderIds(List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        return transactionRepository.findByOrderIdIn(orderIds).stream()
                .filter(transaction -> METHOD_COD.equalsIgnoreCase(transaction.getMethod()))
                .map(this::toRevenueCandidate)
                .toList();
    }

    private PaymentRevenueCandidate toRevenueCandidate(Transaction transaction) {
        return PaymentRevenueCandidate.builder()
                .orderId(transaction.getOrderId())
                .method(transaction.getMethod())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .paidAt(transaction.getPaidAt())
                .build();
    }

    private String buildPaymentToken(String transactionId) {
        String compactId = transactionId == null ? StringUtil.generateShortId() : transactionId;
        return ("SM" + compactId.substring(0, Math.min(compactId.length(), 10))).toUpperCase();
    }

    private String buildTransferContent(String paymentToken) {
        String normalizedBankPrefix = normalizeTransferPart(bankHubTransferPrefix);
        String normalizedPaymentPrefix = normalizeTransferPart(paymentCodePrefix);
        String normalizedPaymentToken = normalizeTransferPart(paymentToken);

        // Keep the Bank Hub marker separate from the StyleMind reference prefix.
        // If deployment configuration repeats the marker in the app prefix, strip
        // only that duplicate leading marker rather than generating SEVQR SEVQR.
        if (!normalizedBankPrefix.isEmpty() && normalizedPaymentPrefix.equals(normalizedBankPrefix)) {
            normalizedPaymentPrefix = "";
        } else if (!normalizedBankPrefix.isEmpty()
                && normalizedPaymentPrefix.startsWith(normalizedBankPrefix + " ")) {
            normalizedPaymentPrefix = normalizedPaymentPrefix.substring(normalizedBankPrefix.length()).trim();
        }

        return java.util.stream.Stream.of(normalizedBankPrefix, normalizedPaymentPrefix, normalizedPaymentToken)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining(" "));
    }

    private String normalizeTransferPart(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private Transaction findPendingSepayTransactionByContent(List<String> rawContents) {
        return transactionRepository.findByMethodAndStatus(METHOD_SEPAY, STATUS_PENDING).stream()
                .filter(t -> rawContents.stream()
                        .anyMatch(rawContent -> paymentReferenceMatcher.matches(t.getTransferContent(), rawContent)))
                .findFirst()
                .orElse(null);
    }

    private Transaction findExpiredSepayTransactionByContent(List<String> rawContents) {
        return transactionRepository.findByMethodAndStatusIn(METHOD_SEPAY, List.of(STATUS_EXPIRED, STATUS_FAILED, "CANCELLED")).stream()
                .filter(t -> rawContents.stream()
                        .anyMatch(rawContent -> paymentReferenceMatcher.matches(t.getTransferContent(), rawContent)))
                .findFirst()
                .orElse(null);
    }

    private String gatewayTransactionId(SepayWebhookPayload payload) {
        return payload.getId() == null ? null : String.valueOf(payload.getId());
    }

    private String rawTransferContent(SepayWebhookPayload payload) {
        String joined = String.join(" | ", rawTransferContents(payload));
        return joined.length() <= 200 ? joined : joined.substring(0, 200);
    }

    private List<String> rawTransferContents(SepayWebhookPayload payload) {
        if (payload == null) {
            return List.of();
        }
        return java.util.stream.Stream.of(
                        payload.getCode(),
                        payload.getContent(),
                        payload.getDescription(),
                        payload.getReferenceCode())
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
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
                "%s/%s-%s-compact2.png?amount=%s&addInfo=%s&accountName=%s",
                vietQrBaseUrl,
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
        String transferContent = transaction.getTransferContent();
        String qrContent = null;
        String qrImageUrl = null;
        if (METHOD_SEPAY.equalsIgnoreCase(transaction.getMethod()) && transferContent != null) {
            qrContent = buildQrContent(transaction.getAmount(), transferContent);
            qrImageUrl = buildQrImageUrl(transaction.getAmount(), transferContent);
        }

        return PaymentResponse.builder()
                .transactionId(transaction.getId())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .method(transaction.getMethod())
                .transactionRef(transaction.getTransactionRef())
                .gatewayTransactionId(transaction.getGatewayTransactionId())
                .paidAt(transaction.getPaidAt() == null
                        ? null
                        : transaction.getPaidAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .transferContent(transferContent)
                .qrContent(qrContent)
                .qrImageUrl(qrImageUrl)
                .expiresAt(transaction.getExpiresAt() == null
                        ? null
                        : transaction.getExpiresAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .build();
    }
}
