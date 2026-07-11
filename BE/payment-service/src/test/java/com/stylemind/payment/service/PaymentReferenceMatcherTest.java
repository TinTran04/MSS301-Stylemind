package com.stylemind.payment.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentReferenceMatcherTest {

    private final PaymentReferenceMatcher matcher = new PaymentReferenceMatcher();

    @Test
    void matches_exactNormalizedContent() {
        assertThat(matcher.matches("STYLEMIND SMABC1234", "STYLEMIND SMABC1234")).isTrue();
    }

    @Test
    void matchesFullBankHubAndStyleMindContent() {
        assertThat(matcher.matches("SEVQR STYLEMIND SMABC1234", "SEVQR STYLEMIND SMABC1234")).isTrue();
    }

    @Test
    void matches_whenIncomingHasExtraSpacesAndDifferentCase() {
        assertThat(matcher.matches("STYLEMIND SMABC1234", "   stylemind   smabc1234  ")).isTrue();
    }

    @Test
    void doesNotMatchSupersetToken() {
        assertThat(matcher.matches("STYLEMIND ORD123", "STYLEMIND ORD1234")).isFalse();
    }

    @Test
    void doesNotMatchDifferentToken() {
        assertThat(matcher.matches("STYLEMIND SMABC1234", "STYLEMIND SMXYZ9999")).isFalse();
    }

    @Test
    void matchesTokenOnlyFieldUsedByWebhookCode() {
        assertThat(matcher.matches("STYLEMIND SME61D2372F2", "SME61D2372F2")).isTrue();
    }

    @Test
    void doesNotMatchSharedBankHubPrefixWithoutUniqueReference() {
        assertThat(matcher.matches("SEVQR STYLEMIND SMABC1234", "SEVQR")).isFalse();
    }

    @Test
    void wrongStyleMindReferenceDoesNotMatchFullContent() {
        assertThat(matcher.matches("SEVQR STYLEMIND SMABC1234", "SEVQR STYLEMIND SMXYZ9999")).isFalse();
    }
}
