package com.stylemind.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = VietnameseLocationValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidVietnameseLocation {
    String message() default "Thành phố / Tỉnh thành phải thuộc 63 Tỉnh Thành hợp lệ ở Việt Nam";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
