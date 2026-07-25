package com.stylemind.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = VietnamesePhoneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidVietnamesePhone {
    String message() default "Số điện thoại Việt Nam không hợp lệ (Ví dụ hợp lệ: 0901234567 hoặc +84901234567)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
