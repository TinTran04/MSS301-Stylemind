package com.stylemind.user.service;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.user.dto.AdministrativeProvinceResponse;
import com.stylemind.user.dto.AdministrativeWardResponse;
import com.stylemind.user.entity.AdministrativeProvince;
import com.stylemind.user.entity.AdministrativeWard;
import com.stylemind.user.repository.AdministrativeProvinceRepository;
import com.stylemind.user.repository.AdministrativeWardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdministrativeDataService {

    private final AdministrativeProvinceRepository provinceRepository;
    private final AdministrativeWardRepository wardRepository;

    public List<AdministrativeProvinceResponse> getProvinces() {
        return provinceRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(province -> AdministrativeProvinceResponse.builder()
                        .code(province.getCode())
                        .name(province.getName())
                        .type(province.getType())
                        .build())
                .toList();
    }

    public List<AdministrativeWardResponse> getWards(String provinceCode) {
        requireProvince(provinceCode);
        return wardRepository.findByProvinceCodeAndActiveTrueOrderByNameAsc(provinceCode).stream()
                .map(ward -> AdministrativeWardResponse.builder()
                        .code(ward.getCode())
                        .provinceCode(ward.getProvinceCode())
                        .name(ward.getName())
                        .type(ward.getType())
                        .build())
                .toList();
    }

    public AdministrativeProvince requireProvince(String provinceCode) {
        return provinceRepository.findById(provinceCode)
                .filter(AdministrativeProvince::isActive)
                .orElseThrow(() -> new BusinessException(
                        "INVALID_PROVINCE_CODE", "Tỉnh/thành phố không hợp lệ", 400));
    }

    public AdministrativeWard requireWard(String wardCode) {
        return wardRepository.findById(wardCode)
                .filter(AdministrativeWard::isActive)
                .orElseThrow(() -> new BusinessException(
                        "INVALID_WARD_CODE", "Phường/xã không hợp lệ", 400));
    }

    public AddressAdministrativeSnapshot validateAndResolve(String provinceCode, String wardCode) {
        AdministrativeProvince province = requireProvince(provinceCode);
        AdministrativeWard ward = requireWard(wardCode);
        if (!province.getCode().equals(ward.getProvinceCode())) {
            throw new BusinessException(
                    "WARD_PROVINCE_MISMATCH", "Phường/xã không thuộc tỉnh/thành phố đã chọn", 400);
        }
        return new AddressAdministrativeSnapshot(
                province.getCode(), province.getName(), ward.getCode(), ward.getName(), province.getDataVersion());
    }

    public record AddressAdministrativeSnapshot(
            String provinceCode,
            String provinceName,
            String wardCode,
            String wardName,
            String dataVersion) {
    }
}
