package com.stylemind.user.service;

import com.stylemind.user.dto.AdministrativeProvinceResponse;
import com.stylemind.user.dto.AdministrativeWardResponse;
import com.stylemind.user.entity.AdministrativeProvince;
import com.stylemind.user.entity.AdministrativeWard;

import java.util.List;

public interface AdministrativeDataService {

    List<AdministrativeProvinceResponse> getProvinces();

    List<AdministrativeWardResponse> getWards(String provinceCode);

    AdministrativeProvince requireProvince(String provinceCode);

    AdministrativeWard requireWard(String wardCode);

    AddressAdministrativeSnapshot validateAndResolve(String provinceCode, String wardCode);

    record AddressAdministrativeSnapshot(
            String provinceCode,
            String provinceName,
            String wardCode,
            String wardName,
            String dataVersion) {
    }
}
