package com.stylemind.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class VietnamesePhoneValidator implements ConstraintValidator<ValidVietnamesePhone, String> {

    // Regex format for Vietnamese Mobile Phone Numbers (Viettel, Vina, Mobi, Vietnamobile, Gmobile, Itelecom)
    // Accepts 10-digit starting with 03, 05, 07, 08, 09 or +84
    private static final Pattern VN_PHONE_PATTERN = Pattern.compile(
        "^(?:(?:\\+84)|0)(?:3[2-9]|5[25689]|7[06-9]|8[1-9]|9[0-9])[0-9]{7}$"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            // Null or blank handling is deferred to @NotBlank if present
            return true;
        }

        // Clean spaces and hyphens
        String cleaned = value.replaceAll("[\\s\\-\\.]", "");
        return VN_PHONE_PATTERN.matcher(cleaned).matches();
    }
}
