package com.stylemind.payment.service;

public interface PaymentReferenceMatcher {

    boolean matches(String expectedTransferContent, String incomingContent);

    String normalize(String value);

    String extractPaymentToken(String normalizedContent);
}
