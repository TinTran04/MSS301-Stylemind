package com.stylemind.product.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProductVariantRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private ProductVariantRequest validRequest() {
        return ProductVariantRequest.builder()
                .sku("SKU-1")
                .size("M")
                .color("Đen")
                .stockQuantity(5)
                .build();
    }

    @Test
    void validRequest_hasNoViolations() {
        assertThat(validator.validate(validRequest())).isEmpty();
    }

    @Test
    void negativeStockQuantity_isRejected() {
        ProductVariantRequest request = validRequest();
        request.setStockQuantity(-1);

        Set<ConstraintViolation<ProductVariantRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("stockQuantity"));
    }

    @Test
    void missingStockQuantity_isRejected() {
        ProductVariantRequest request = validRequest();
        request.setStockQuantity(null);

        Set<ConstraintViolation<ProductVariantRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("stockQuantity"));
    }

    @Test
    void zeroStockQuantity_isAccepted() {
        ProductVariantRequest request = validRequest();
        request.setStockQuantity(0);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void priceOverrideMustBePositiveWhenProvided() {
        ProductVariantRequest request = validRequest();
        request.setPriceOverride(BigDecimal.ZERO);

        Set<ConstraintViolation<ProductVariantRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("priceOverride"));
    }
}
