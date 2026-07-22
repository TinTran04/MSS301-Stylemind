package com.stylemind.payment.service;

import com.stylemind.payment.dto.CodCheckoutRequest;
import com.stylemind.payment.dto.PaymentResponse;
import com.stylemind.payment.dto.SepayCheckoutRequest;
import com.stylemind.payment.dto.SepayWebhookPayload;

public interface PaymentService {

    PaymentResponse createCodPayment(CodCheckoutRequest request);

    PaymentResponse createSepayPayment(SepayCheckoutRequest request);

    PaymentResponse getPaymentStatus(String orderId);

    void processSepayWebhook(String authorizationHeader, SepayWebhookPayload payload);

    void expirePendingSepayPayment(String orderId);

    void refund(String transactionId);
}
