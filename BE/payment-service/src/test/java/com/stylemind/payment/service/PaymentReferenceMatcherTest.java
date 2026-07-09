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
}
