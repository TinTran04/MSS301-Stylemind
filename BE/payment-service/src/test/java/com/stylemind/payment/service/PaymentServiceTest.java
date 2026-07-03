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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock PaymentWebhookEventRepository webhookEventRepository;
    @Mock OrderClient orderClient;

    @InjectMocks
    PaymentService paymentService;

    // @Value fields are never populated without a Spring context in a plain
    // Mockito unit test - set them manually to mirror application.yml defaults.
    @BeforeEach
    void setUpDefaults() {
        ReflectionTestUtils.setField(paymentService, "vietQrBankId", "970436");
        ReflectionTestUtils.setField(paymentService, "vietQrAccountNo", "0123456789");
        ReflectionTestUtils.setField(paymentService, "vietQrAccountName", "STYLEMIND SANDBOX");
        ReflectionTestUtils.setField(paymentService, "vietQrExpiryMinutes", 15L);
        ReflectionTestUtils.setField(paymentService, "sepayWebhookApiKey", "test-webhook-key");
    }

    @Test
    void createCodPayment_returnsPendingWithNoQrPayload() {
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.createCodPayment(
                CodCheckoutRequest.builder().orderId("order-1").amount(new BigDecimal("100000")).build());

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getQrImageUrl()).isNull();
        assertThat(response.getTransferContent()).isNull();
    }

    @Test
    void createSepayPayment_returnsVietQrPayloadWithUniqueTransferContentAndExpiry() {
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.createSepayPayment(
                SepayCheckoutRequest.builder().orderId("order-1").amount(new BigDecimal("100000")).build());

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getTransferContent()).isEqualTo("STYLEMIND ORDorder-1");
        assertThat(response.getQrImageUrl()).startsWith("https://img.vietqr.io/image/");
        assertThat(response.getQrContent()).contains("100000").contains("STYLEMIND ORDorder-1");
        assertThat(response.getExpiresAt()).isAfter(java.time.Instant.now());
    }

    @Test
    void webhook_rejectsMissingOrWrongApiKey() {
        SepayWebhookPayload payload = webhookPayload(1L, "STYLEMIND ORDorder-1", "in", "100000");

        assertThatThrownBy(() -> paymentService.processSepayWebhook("Apikey wrong-key", payload))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid webhook credentials");

        verify(webhookEventRepository).save(argThatResult("INVALID_SIGNATURE"));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void webhook_missingGatewayTransactionId_rejectedAsInvalidPayload() {
        SepayWebhookPayload payload = webhookPayload(null, "STYLEMIND ORDorder-1", "in", "100000");

        assertThatThrownBy(() -> paymentService.processSepayWebhook("Apikey test-webhook-key", payload))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("missing an id");

        verify(webhookEventRepository).save(argThatResult("INVALID_PAYLOAD"));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void webhook_duplicateGatewayTransactionId_isIgnored() {
        when(webhookEventRepository.findByGatewayTransactionId("42"))
                .thenReturn(Optional.of(com.stylemind.payment.entity.PaymentWebhookEvent.builder().id("evt-1").build()));

        paymentService.processSepayWebhook("Apikey test-webhook-key", webhookPayload(42L, "STYLEMIND ORDorder-1", "in", "100000"));

        verify(transactionRepository, never()).save(any());
        verify(webhookEventRepository, never()).save(any());
    }

    @Test
    void webhook_matchingContentAndAmount_marksTransactionCompletedAndNotifiesOrderService() {
        Transaction pending = pendingSepayTransaction("txn-1", "order-1", "STYLEMIND ORDorder-1", "100000");
        when(webhookEventRepository.findByGatewayTransactionId("42")).thenReturn(Optional.empty());
        when(transactionRepository.findByMethodAndStatus("sepay", "PENDING")).thenReturn(List.of(pending));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Banks routinely mangle whitespace/case around the note - the real
        // webhook content need not match byte-for-byte with what we generated.
        paymentService.processSepayWebhook("Apikey test-webhook-key",
                webhookPayload(42L, "NGUYEN VAN A chuyen tien stylemind ord order-1", "in", "100000"));

        assertThat(pending.getStatus()).isEqualTo("COMPLETED");
        assertThat(pending.getGatewayTransactionId()).isEqualTo("42");
        verify(webhookEventRepository).save(argThatResult("MATCHED"));
        verify(orderClient).updatePaymentStatus(eq("order-1"),
                argThat(r -> "PAID".equals(r.getStatus())));
    }

    @Test
    void webhook_amountMismatch_marksTransactionFailedAndNotifiesOrderService() {
        Transaction pending = pendingSepayTransaction("txn-1", "order-1", "STYLEMIND ORDorder-1", "100000");
        when(webhookEventRepository.findByGatewayTransactionId("42")).thenReturn(Optional.empty());
        when(transactionRepository.findByMethodAndStatus("sepay", "PENDING")).thenReturn(List.of(pending));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processSepayWebhook("Apikey test-webhook-key",
                webhookPayload(42L, "STYLEMIND ORDorder-1", "in", "50000"));

        assertThat(pending.getStatus()).isEqualTo("FAILED");
        verify(webhookEventRepository).save(argThatResult("AMOUNT_MISMATCH"));
        verify(orderClient).updatePaymentStatus(eq("order-1"),
                argThat(r -> "FAILED".equals(r.getStatus())));
    }

    @Test
    void webhook_noMatchingOrder_logsAndDoesNotThrow() {
        when(webhookEventRepository.findByGatewayTransactionId("42")).thenReturn(Optional.empty());
        when(transactionRepository.findByMethodAndStatus("sepay", "PENDING")).thenReturn(List.of());

        paymentService.processSepayWebhook("Apikey test-webhook-key",
                webhookPayload(42L, "some unrelated transfer note", "in", "100000"));

        verify(webhookEventRepository).save(argThatResult("NO_MATCHING_ORDER"));
        verify(orderClient, never()).updatePaymentStatus(any(), any());
    }

    @Test
    void webhook_outboundTransfer_isIgnored() {
        when(webhookEventRepository.findByGatewayTransactionId("42")).thenReturn(Optional.empty());

        paymentService.processSepayWebhook("Apikey test-webhook-key",
                webhookPayload(42L, "STYLEMIND ORDorder-1", "out", "100000"));

        verify(webhookEventRepository).save(argThatResult("IGNORED_OUTBOUND"));
        verify(transactionRepository, never()).findByMethodAndStatus(any(), any());
    }

    private com.stylemind.payment.entity.PaymentWebhookEvent argThatResult(String expectedResult) {
        return argThat(evt -> expectedResult.equals(evt.getResult()));
    }

    private SepayWebhookPayload webhookPayload(Long id, String content, String transferType, String amount) {
        return SepayWebhookPayload.builder()
                .id(id)
                .gateway("MBBank")
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
