package com.stylemind.user.validation;

import com.stylemind.user.dto.DeliveryAddressRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class VietnameseValidationTest {

    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void testValidVietnamesePhoneNumbers() {
        String[] validPhones = {
            "0901234567",
            "0381234567",
            "0709876543",
            "0868888888",
            "0562345678",
            "+84901234567",
            "+84381234567"
        };

        for (String phone : validPhones) {
            DeliveryAddressRequest req = DeliveryAddressRequest.builder()
                .recipientName("Nguyễn Văn A")
                .phoneNumber(phone)
                .addressLine("123 Đường Nguyễn Huệ")
                .provinceCode("79")
                .wardCode("26740")
                .city("TP. Hồ Chí Minh")
                .build();

            Set<ConstraintViolation<DeliveryAddressRequest>> phoneViolations = validator.validate(req).stream()
                .filter(v -> v.getPropertyPath().toString().equals("phoneNumber"))
                .collect(Collectors.toSet());

            assertTrue(phoneViolations.isEmpty(), "Expected valid phone: " + phone);
        }
    }

    @Test
    public void testInvalidVietnamesePhoneNumbers() {
        String[] invalidPhones = {
            "0123456789", // 01x is deprecated
            "09012345",   // too short
            "1234567890", // doesn't start with 0 or +84
            "abcdefghij", // letters
            "090123456789" // too long
        };

        for (String phone : invalidPhones) {
            DeliveryAddressRequest req = DeliveryAddressRequest.builder()
                .recipientName("Nguyễn Văn A")
                .phoneNumber(phone)
                .addressLine("123 Đường Nguyễn Huệ")
                .provinceCode("01")
                .wardCode("00001")
                .city("Hà Nội")
                .build();

            Set<ConstraintViolation<DeliveryAddressRequest>> phoneViolations = validator.validate(req).stream()
                .filter(v -> v.getPropertyPath().toString().equals("phoneNumber"))
                .collect(Collectors.toSet());

            assertFalse(phoneViolations.isEmpty(), "Expected invalid phone: " + phone);
        }
    }

    @Test
    public void testValidVietnameseLocations() {
        String[] validCities = {
            "Hà Nội",
            "TP. Hồ Chí Minh",
            "Đà Nẵng",
            "Hải Phòng",
            "Cần Thơ",
            "Tỉnh An Giang",
            "Thành phố Đà Nẵng",
            "Bà Rịa - Vũng Tàu",
            "Lâm Đồng",
            "Hồ Chí Minh"
        };

        for (String city : validCities) {
            DeliveryAddressRequest req = DeliveryAddressRequest.builder()
                .recipientName("Trần Thị B")
                .phoneNumber("0987654321")
                .addressLine("456 Lê Lợi")
                .provinceCode("01")
                .wardCode("00001")
                .city(city)
                .build();

            Set<ConstraintViolation<DeliveryAddressRequest>> locationViolations = validator.validate(req).stream()
                .filter(v -> v.getPropertyPath().toString().equals("city"))
                .collect(Collectors.toSet());

            assertTrue(locationViolations.isEmpty(), "Expected valid location: " + city);
        }
    }

    @Test
    public void testInvalidVietnameseLocations() {
        String[] invalidCities = {
            "New York",
            "Tokyo",
            "Abcxyz City",
            "Tỉnh Không Tồn Tại",
            "12345"
        };

        for (String city : invalidCities) {
            DeliveryAddressRequest req = DeliveryAddressRequest.builder()
                .recipientName("Trần Thị B")
                .phoneNumber("0987654321")
                .addressLine("456 Lê Lợi")
                .provinceCode("01")
                .wardCode("00001")
                .city(city)
                .build();

            Set<ConstraintViolation<DeliveryAddressRequest>> locationViolations = validator.validate(req).stream()
                .filter(v -> v.getPropertyPath().toString().equals("city"))
                .collect(Collectors.toSet());

            assertFalse(locationViolations.isEmpty(), "Expected invalid location: " + city);
        }
    }
}
