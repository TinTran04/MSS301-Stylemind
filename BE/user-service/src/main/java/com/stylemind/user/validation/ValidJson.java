package com.stylemind.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that the annotated String is either null/blank (optional field)
 * or a syntactically valid JSON value (object, array, or primitive).
 */
@Documented
@Constraint(validatedBy = JsonValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidJson {
    String message() default "Giá trị phải là JSON hợp lệ";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
