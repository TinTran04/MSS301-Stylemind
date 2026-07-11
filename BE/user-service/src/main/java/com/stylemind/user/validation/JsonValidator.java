package com.stylemind.user.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

public class JsonValidator implements ConstraintValidator<ValidJson, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null or blank is allowed — field is optional
        if (!StringUtils.hasText(value)) {
            return true;
        }
        try {
            MAPPER.readTree(value);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
