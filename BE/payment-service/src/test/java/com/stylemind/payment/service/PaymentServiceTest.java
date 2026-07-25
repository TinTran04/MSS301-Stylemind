package com.stylemind.payment.service;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.payment.dto.CodCheckoutRequest;
import com.stylemind.payment.dto.PaymentResponse;
import com.stylemind.payment.dto.SepayCheckoutRequest;
import com.stylemind.payment.dto.SepayWebhookPayload;
import com.stylemind.payment.entity.Transaction;
import com.stylemind.payment.feign.OrderClient;
import com.stylemind.payment.repository.PaymentWebhookEventRepository;
import com.stylemind.payment.repository.TransactionRepository;
import com.stylemind.payment.service.impl.PaymentReferenceMatcherImpl;
import com.stylemind.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock PaymentWebhookEventRepository webhookEventRepository;
    @Mock OrderClient orderClient;
    @Spy PaymentReferenceMatcher paymentReferenceMatcher = new PaymentReferenceMatcherImpl();

    @InjectMocks
    PaymentServiceImpl paymentService;

    // @Value fields are never populated without a Spring context in a plain
    // Mockito unit test - set them manually to mirror application.yml defaults.
    @BeforeEach
    void setUpDefaults() {
        ReflectionTestUtils.setField(paymentService, "vietQrBankId", "970415");
        ReflectionTestUtils.setField(paymentService, "vietQrAccountNo", "0123456789");
        ReflectionTestUtils.setField(paymentService, "vietQrAccountName", "STYLEMIND SANDBOX");
        ReflectionTestUtils.setField(paymentService, "bankHubTransferPrefix", "SEVQR");
        ReflectionTestUtils.setField(paymentService, "vietQrExpiryMinutes", 15L);
        ReflectionTestUtils.setField(paymentService, "vietQrBaseUrl", "https://img.vietqr.io/image");
        ReflectionTestUtils.setField(paymentService, "paymentCodePrefix", "STYLEMIND");
        ReflectionTestUtils.setField(paymentService, "sepayWebhookApiKey", "test-webhook-key");
    }

    @Test
    void createCodPayment_returnsPendingWithNoQrPayload() {
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.createCodPayment(
                CodCheckoutRequest.builder().orderId("order-1").userId("user-1").amount(new BigDecimal("100000")).build());

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getQrImageUrl()).isNull();
        assertThat(response.getTransferContent()).isNull();
    }

    @Test
    void createSepayPayment_returnsVietQrPayloadWithUniqueTransferContentAndExpiry() {
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.createSepayPayment(
                SepayCheckoutRequest.builder().orderId("order-1").userId("user-1").amount(new BigDecimal("100000")).build());

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getTransferContent()).startsWith("SEVQR STYLEMIND SM");
        assertThat(response.getQrImageUrl()).startsWith("https://img.vietqr.io/image/");
        assertThat(response.getQrContent()).contains("100000").contains(response.getTransferContent());
        assertThat(response.getQrImageUrl()).contains("addInfo="
                + java.net.URLEncoder.encode(response.getTransferContent(), java.nio.charset.StandardCharsets.UTF_8));
        verify(transactionRepository).save(argThat(transaction ->
                response.getTransferContent().equals(transaction.getTransferContent())));
        assertThat(response.getExpiresAt()).isAfter(java.time.Instant.now());
    }

    @Test
    void createSepayPayment_doesNotDuplicateBankHubPrefix() {
        ReflectionTestUtils.setField(paymentService, "paymentCodePrefix", "SEVQR STYLEMIND");
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.createSepayPayment(
                SepayCheckoutRequest.builder().orderId("order-1").userId("user-1").amount(new BigDecimal("100000")).build());

        assertThat(response.getTransferContent()).startsWith("SEVQR STYLEMIND SM");
        assertThat(response.getTransferContent()).doesNotContain("SEVQR SEVQR");
    }

    @Test
    void revenueCandidates_rejectNullOrNonIncreasingBoundsBeforeRepositoryAccess() {
        assertThatThrownBy(() -> paymentService.findSepayRevenueCandidates(null, LocalDateTime.now()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Khoảng thời gian doanh thu không hợp lệ");

        assertThatThrownBy(() -> paymentService.findSepayRevenueCandidates(
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 7, 1, 0, 0)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Khoảng thời gian doanh thu không hợp lệ");

        verify(transactionRepository, never()).findSepayRevenueCandidates(any(), any());
    }

    @Test
    void webhook_rejectsMissingOrWrongApiKey() {
        SepayWebhookPayload payload = webhookPayload(1L, "STYLEMIND SMABC1234", "in", "100000");

        assertThatThrownBy(() -> paymentService.processSepayWebhook("Apikey wrong-key", payload))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid webhook credentials");

        verify(webhookEventRepository).save(argThatResult("INVALID_SIGNATURE"));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void webhook_missingGatewayTransactionId_rejectedAsInvalidPayload() {
        SepayWebhookPayload payload = webhookPayload(null, "STYLEMIND SMABC1234", "in", "100000");

        assertThatThrownBy(() -> paymentService.processSepayWebhook("Apikey test-webhook-key", payload))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("missing an id");

        verify(webhookEventRepository).save(argThatResult("INVALID_PAYLOAD"));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void webhook_duplicateGatewayTransactionId_isIgnored() {
        when(webhookEventRepository.findByProviderAndGatewayTransactionId("SEPAY", "42"))
                .thenReturn(Optional.of(com.stylemind.payment.entity.PaymentWebhookEvent.builder().id("evt-1").build()));

        paymentService.processSepayWebhook("Apikey test-webhook-key", webhookPayload(42L, "STYLEMIND SMABC1234", "in", "100000"));

        verify(transactionRepository, never()).save(any());
        verify(webhookEventRepository, never()).save(any());
    }

    @Test
    void webhook_matchingContentAndAmount_marksTransactionCompletedAndNotifiesOrderService() {
        Transaction pending = pendingSepayTransaction("txn-1", "order-1", "STYLEMIND SMABC1234", "100000");
        when(webhookEventRepository.findByProviderAndGatewayTransactionId("SEPAY", "42")).thenReturn(Optional.empty());
        when(transactionRepository.findByMethodAndStatus("sepay", "PENDING")).thenReturn(List.of(pending));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processSepayWebhook("Apikey test-webhook-key",
                webhookPayload(42L, "  nguyen van a chuyen tien stylemind   smabc1234  ", "in", "100000"));

        assertThat(pending.getStatus()).isEqualTo("PAID");
        assertThat(pending.getGatewayTransactionId()).isEqualTo("42");
        verify(webhookEventRepository, atLeastOnce()).save(argThatResult("MATCHED"));
        verify(orderClient).updatePaymentStatus(eq("order-1"),
                argThat(r -> "PAID".equals(r.getStatus())));
    }

    @Test
    void webhook_matchingCodeField_marksTransferContentTransactionPaid() {
        Transaction pending = pendingSepayTransaction("txn-1", "order-1", "STYLEMIND SME61D2372F2", "100000");
        when(webhookEventRepository.findByProviderAndGatewayTransactionId("SEPAY", "42")).thenReturn(Optional.empty());
        when(transactionRepository.findByMethodAndStatus("sepay", "PENDING")).thenReturn(List.of(pending));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processSepayWebhook("Apikey test-webhook-key",
                webhookPayloadWithCode(42L, "SME61D2372F2", null, "in", "100000"));

        assertThat(pending.getStatus()).isEqualTo("PAID");
        assertThat(pending.getGatewayTransactionId()).isEqualTo("42");
        assertThat(pending.getPaidAt()).isNotNull();
        verify(orderClient).updatePaymentStatus(eq("order-1"), argThat(r -> "PAID".equals(r.getStatus())));
    }

    @Test
    void webhook_similarReferenceDoesNotMatchDifferentOrder() {
        Transaction pending = pendingSepayTransaction("txn-1", "order-1", "STYLEMIND ORD123", "100000");
        when(webhookEventRepository.findByProviderAndGatewayTransactionId("SEPAY", "42")).thenReturn(Optional.empty());
        when(transactionRepository.findByMethodAndStatus("sepay", "PENDING")).thenReturn(List.of(pending));
        when(transactionRepository.findByMethodAndStatusIn("sepay", List.of("EXPIRED", "FAILED", "CANCELLED")))
                .thenReturn(List.of());
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processSepayWebhook("Apikey test-webhook-key",
                webhookPayload(42L, "STYLEMIND ORD1234", "in", "100000"));

        assertThat(pending.getStatus()).isEqualTo("PENDING");
        verify(webhookEventRepository, atLeastOnce()).save(argThatResultAndProcessed("NO_MATCHING_ORDER", false));
        verify(orderClient, never()).updatePaymentStatus(any(), any());
    }

    @Test
    void webhook_amountMismatch_marksTransactionFailedAndNotifiesOrderService() {
        Transaction pending = pendingSepayTransaction("txn-1", "order-1", "STYLEMIND SMABC1234", "100000");
        when(webhookEventRepository.findByProviderAndGatewayTransactionId("SEPAY", "42")).thenReturn(Optional.empty());
        when(transactionRepository.findByMethodAndStatus("sepay", "PENDING")).thenReturn(List.of(pending));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processSepayWebhook("Apikey test-webhook-key",
                webhookPayload(42L, "STYLEMIND SMABC1234", "in", "50000"));

        assertThat(pending.getStatus()).isEqualTo("FAILED");
        verify(webhookEventRepository, atLeastOnce()).save(argThatResultAndProcessed("AMOUNT_MISMATCH", false));
        verify(orderClient).updatePaymentStatus(eq("order-1"),
                argThat(r -> "FAILED".equals(r.getStatus())));
    }

    @Test
    void webhook_noMatchingOrder_logsAndDoesNotThrow() {
        when(webhookEventRepository.findByProviderAndGatewayTransactionId("SEPAY", "42")).thenReturn(Optional.empty());
        when(transactionRepository.findByMethodAndStatus("sepay", "PENDING")).thenReturn(List.of());
        when(transactionRepository.findByMethodAndStatusIn("sepay", List.of("EXPIRED", "FAILED", "CANCELLED")))
                .thenReturn(List.of());
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processSepayWebhook("Apikey test-webhook-key",
                webhookPayload(42L, "some unrelated transfer note", "in", "100000"));

        verify(webhookEventRepository, atLeastOnce()).save(argThatResult("NO_MATCHING_ORDER"));
        verify(orderClient, never()).updatePaymentStatus(any(), any());
    }

    @Test
    void webhook_outboundTransfer_isIgnored() {
        when(webhookEventRepository.findByProviderAndGatewayTransactionId("SEPAY", "42")).thenReturn(Optional.empty());
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processSepayWebhook("Apikey test-webhook-key",
                webhookPayload(42L, "STYLEMIND SMABC1234", "out", "100000"));

        verify(webhookEventRepository, atLeastOnce()).save(argThatResult("IGNORED_OUTBOUND"));
        verify(transactionRepository, never()).findByMethodAndStatus(any(), any());
    }

    @Test
    void webhook_lateAfterExpiration_isRecordedAndDoesNotNotifyOrderService() {
        Transaction expired = pendingSepayTransaction("txn-1", "order-1", "STYLEMIND SMABC1234", "100000");
        expired.setStatus("EXPIRED");
        when(webhookEventRepository.findByProviderAndGatewayTransactionId("SEPAY", "42")).thenReturn(Optional.empty());
        when(transactionRepository.findByMethodAndStatus("sepay", "PENDING")).thenReturn(List.of());
        when(transactionRepository.findByMethodAndStatusIn("sepay", List.of("EXPIRED", "FAILED", "CANCELLED")))
                .thenReturn(List.of(expired));
        when(webhookEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processSepayWebhook("Apikey test-webhook-key",
                webhookPayload(42L, "STYLEMIND SMABC1234", "in", "100000"));

        verify(webhookEventRepository, atLeastOnce()).save(argThatResultAndProcessed("LATE_AFTER_EXPIRY", false));
        verify(orderClient, never()).updatePaymentStatus(any(), any());
    }

    @Test
    void expirePendingSepayPayment_marksPendingTransactionExpired() {
        Transaction pending = pendingSepayTransaction("txn-1", "order-1", "STYLEMIND SMABC1234", "100000");
        when(transactionRepository.findTopByOrderIdOrderByCreatedAtDesc("order-1")).thenReturn(Optional.of(pending));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.expirePendingSepayPayment("order-1");

        assertThat(pending.getStatus()).isEqualTo("EXPIRED");
    }

    private com.stylemind.payment.entity.PaymentWebhookEvent argThatResult(String expectedResult) {
        return argThat(evt -> expectedResult.equals(evt.getResult()));
    }

    private com.stylemind.payment.entity.PaymentWebhookEvent argThatResultAndProcessed(
            String expectedResult, boolean expectedProcessed) {
        return argThat(evt -> expectedResult.equals(evt.getResult())
                && Boolean.valueOf(expectedProcessed).equals(evt.getProcessed()));
    }

    private SepayWebhookPayload webhookPayload(Long id, String content, String transferType, String amount) {
        return webhookPayloadWithCode(id, null, content, transferType, amount);
    }

    private SepayWebhookPayload webhookPayloadWithCode(Long id, String code, String content, String transferType, String amount) {
        return SepayWebhookPayload.builder()
                .id(id)
                .gateway("MBBank")
                .code(code)
                .content(content)
                .transferType(transferType)
                .transferAmount(new BigDecimal(amount))
                .build();
    }

    private Transaction pendingSepayTransaction(String id, String orderId, String transferContent, String amount) {
        return Transaction.builder()
                .id(id)
                .orderId(orderId)
                .userId("")
                .amount(new BigDecimal(amount))
                .method("sepay")
                .status("PENDING")
                .transactionRef("ref-1")
                .transferContent(transferContent)
                .build();
    }
}
