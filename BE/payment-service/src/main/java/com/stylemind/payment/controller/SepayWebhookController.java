package com.stylemind.payment.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.payment.dto.SepayWebhookPayload;
import com.stylemind.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

// Public endpoint (no JWT) - SePay's own servers call this directly, not the
// browser. Authenticity is verified inside PaymentService via the Authorization
// header (SePay's static per-integration API key), not by JWT/session state.
// Must be permitted both at the gateway (JwtAuthenticationFilter public paths +
// a routed path) and in this service's own SecurityConfig permitAll list.
@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
@Slf4j
public class SepayWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/sepay")
    public ResponseEntity<ApiResponse<Void>> handleSepayWebhook(
            HttpServletRequest request,
            @RequestBody SepayWebhookPayload payload) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String requestId = request.getHeader("X-Request-Id");
        logWebhookReceipt(requestId, payload, authorizationHeader);
        paymentService.processSepayWebhook(authorizationHeader, payload);
        return ResponseEntity.ok(ApiResponse.success("OK", null));
    }

    private void logWebhookReceipt(String requestId, SepayWebhookPayload payload, String authorizationHeader) {
        log.info("SePay webhook received requestId={} gatewayTransactionId={} authorizationPresent={} "
                        + "fields[id={},code={},content={},description={},referenceCode={},amount={},transferType={}]",
                requestId,
                payload == null ? null : payload.getId(),
                StringUtils.hasText(authorizationHeader),
                payload != null && payload.getId() != null,
                payload != null && StringUtils.hasText(payload.getCode()),
                payload != null && StringUtils.hasText(payload.getContent()),
                payload != null && StringUtils.hasText(payload.getDescription()),
                payload != null && StringUtils.hasText(payload.getReferenceCode()),
                payload != null && payload.getTransferAmount() != null,
                payload != null && StringUtils.hasText(payload.getTransferType()));
    }
}
