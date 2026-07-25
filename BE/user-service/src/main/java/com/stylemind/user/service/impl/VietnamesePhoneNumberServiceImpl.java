package com.stylemind.user.service.impl;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.user.service.VietnamesePhoneNumberService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VietnamesePhoneNumberServiceImpl implements VietnamesePhoneNumberService {

    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    @Override
    public String normalize(String rawPhoneNumber) {
        if (!StringUtils.hasText(rawPhoneNumber)) {
            throw new BusinessException("RECIPIENT_PHONE_REQUIRED", "Số điện thoại người nhận không được để trống", 400);
        }

        try {
            Phonenumber.PhoneNumber parsed = phoneNumberUtil.parse(rawPhoneNumber.trim(), "VN");
            if (!phoneNumberUtil.isValidNumberForRegion(parsed, "VN")) {
                throw invalidPhone();
            }
            return phoneNumberUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException ex) {
            throw invalidPhone();
        }
    }

    private BusinessException invalidPhone() {
        return new BusinessException("INVALID_VN_PHONE", "Số điện thoại Việt Nam không hợp lệ", 400);
    }
}
