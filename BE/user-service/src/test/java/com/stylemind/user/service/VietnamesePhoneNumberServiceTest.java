package com.stylemind.user.service;

import com.stylemind.user.service.impl.VietnamesePhoneNumberServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VietnamesePhoneNumberServiceTest {

    private final VietnamesePhoneNumberService service = new VietnamesePhoneNumberServiceImpl();

    @Test
    void acceptsLocalVietnameseNumberAndNormalizesToE164() {
        assertThat(service.normalize("0912 345 678")).isEqualTo("+84912345678");
    }

    @Test
    void acceptsPlus84VietnameseNumber() {
        assertThat(service.normalize("+84 912 345 678")).isEqualTo("+84912345678");
    }

    @Test
    void rejectsBlankMalformedAndNonVietnameseNumbers() {
        assertThatThrownBy(() -> service.normalize(" "))
                .hasMessageContaining("không được để trống");
        assertThatThrownBy(() -> service.normalize("0123"))
                .hasMessageContaining("không hợp lệ");
        assertThatThrownBy(() -> service.normalize("+14155552671"))
                .hasMessageContaining("không hợp lệ");
    }
}
