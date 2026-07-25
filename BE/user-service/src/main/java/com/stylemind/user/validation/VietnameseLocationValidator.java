package com.stylemind.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.text.Normalizer;
import java.util.*;

public class VietnameseLocationValidator implements ConstraintValidator<ValidVietnameseLocation, String> {

    // Official 63 Administrative Divisions (Provinces and Cities) of Vietnam
    private static final Set<String> VN_PROVINCES_NORMALIZED = new HashSet<>();

    static {
        List<String> rawProvinces = Arrays.asList(
            "An Giang", "Bà Rịa - Vũng Tàu", "Bắc Giang", "Bắc Kạn", "Bạc Liêu",
            "Bắc Ninh", "Bến Tre", "Bình Định", "Bình Dương", "Bình Phước",
            "Bình Thuận", "Cà Mau", "Cần Thơ", "Cao Bằng", "Đà Nẵng",
            "Đắc Lắc", "Đắk Lắk", "Đắc Nông", "Đắk Nông", "Điện Biên", "Đồng Nai",
            "Đồng Tháp", "Gia Lai", "Hà Giang", "Hà Nam", "Hà Nội",
            "Hà Tĩnh", "Hải Dương", "Hải Phòng", "Hậu Giang", "Hòa Bình",
            "Hưng Yên", "Khánh Hòa", "Kiên Giang", "Kon Tum", "Lai Châu",
            "Lâm Đồng", "Lạng Sơn", "Lào Cai", "Long An", "Nam Định",
            "Nghệ An", "Ninh Bình", "Ninh Thuận", "Phú Thọ", "Phú Yên",
            "Quảng Bình", "Quảng Nam", "Quảng Ngãi", "Quảng Ninh", "Quảng Trị",
            "Sóc Trăng", "Sơn La", "Tây Ninh", "Thái Bình", "Thái Nguyên",
            "Thanh Hóa", "Thừa Thiên Huế", "Tiền Giang", "TP. Hồ Chí Minh", "Hồ Chí Minh",
            "TP.HCM", "TPHCM", "Saigon", "Trà Vinh", "Tuyên Quang", "Vĩnh Long",
            "Vĩnh Phúc", "Yên Bái"
        );

        for (String p : rawProvinces) {
            VN_PROVINCES_NORMALIZED.add(normalizeString(p));
        }
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Defer to @NotBlank
        }

        String normalizedInput = normalizeString(value);
        if (VN_PROVINCES_NORMALIZED.contains(normalizedInput)) {
            return true;
        }

        // Strip common prefixes like "tinh", "thanh pho", "tp"
        String strippedInput = normalizedInput
            .replaceAll("^(tinh|thanh pho|tp\\.)\\s*", "")
            .trim();

        for (String validProv : VN_PROVINCES_NORMALIZED) {
            String strippedValid = validProv
                .replaceAll("^(tinh|thanh pho|tp\\.)\\s*", "")
                .trim();
            if (strippedInput.equalsIgnoreCase(strippedValid)) {
                return true;
            }
        }

        return false;
    }

    private static String normalizeString(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input.trim().toLowerCase(), Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        normalized = normalized.replace('đ', 'd').replace('Đ', 'd');
        return normalized;
    }
}
