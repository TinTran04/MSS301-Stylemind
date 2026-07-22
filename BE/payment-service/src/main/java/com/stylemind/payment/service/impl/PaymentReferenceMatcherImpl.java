package com.stylemind.payment.service.impl;

import com.stylemind.payment.service.PaymentReferenceMatcher;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PaymentReferenceMatcherImpl implements PaymentReferenceMatcher {

    private static final Pattern NON_ALNUM = Pattern.compile("[^A-Z0-9]+");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final Pattern STYLEMIND_TOKEN_PATTERN =
            Pattern.compile("(^|\\s)STYLEMIND\\s+([A-Z0-9]{6,64})(\\s|$)");

    @Override
    public boolean matches(String expectedTransferContent, String incomingContent) {
        String normalizedExpected = normalize(expectedTransferContent);
        String normalizedIncoming = normalize(incomingContent);

        if (normalizedExpected.isEmpty() || normalizedIncoming.isEmpty()) {
            return false;
        }

        if (normalizedExpected.equals(normalizedIncoming)) {
            return true;
        }

        String expectedToken = extractPaymentToken(normalizedExpected);
        String incomingToken = extractPaymentToken(normalizedIncoming);
        if (!expectedToken.isBlank() && expectedToken.equals(incomingToken)) {
            return true;
        }

        // SePay may put only the bounded payment token in the `code` field.
        // Compare the complete normalized field, never a substring.
        return !expectedToken.isBlank() && expectedToken.equals(normalizedIncoming);
    }

    @Override
    public String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String uppercased = value.toUpperCase(Locale.ROOT);
        String withSpaces = NON_ALNUM.matcher(uppercased).replaceAll(" ").trim();
        return MULTI_SPACE.matcher(withSpaces).replaceAll(" ");
    }

    @Override
    public String extractPaymentToken(String normalizedContent) {
        if (normalizedContent == null || normalizedContent.isBlank()) {
            return "";
        }

        Matcher matcher = STYLEMIND_TOKEN_PATTERN.matcher(normalizedContent);
        if (!matcher.find()) {
            return "";
        }

        return matcher.group(2);
    }
}
