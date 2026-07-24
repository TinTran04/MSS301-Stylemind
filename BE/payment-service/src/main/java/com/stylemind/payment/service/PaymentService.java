package com.stylemind.payment.service;

import com.stylemind.payment.dto.CodCheckoutRequest;
import com.stylemind.payment.dto.CancelPaymentRequest;
import com.stylemind.payment.dto.PaymentResponse;
import com.stylemind.payment.dto.PaymentCancellationResponse;
import com.stylemind.payment.dto.PaymentRevenueCandidate;
import com.stylemind.payment.dto.SepayCheckoutRequest;
import com.stylemind.payment.dto.SepayWebhookPayload;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {

    PaymentResponse createCodPayment(CodCheckoutRequest request);

    PaymentResponse createSepayPayment(SepayCheckoutRequest request);

    PaymentResponse getPaymentStatus(String orderId);

    void processSepayWebhook(String authorizationHeader, SepayWebhookPayload payload);

    void expirePendingSepayPayment(String orderId);

    PaymentCancellationResponse cancelPayment(String orderId, CancelPaymentRequest request);

    void refund(String transactionId);

    List<PaymentRevenueCandidate> findSepayRevenueCandidates(LocalDateTime from, LocalDateTime to);

    List<PaymentRevenueCandidate> findRevenueCandidatesByOrderIds(List<String> orderIds);
}
