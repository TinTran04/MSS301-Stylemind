package com.stylemind.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stylemind.user.entity.AdministrativeProvince;
import com.stylemind.user.entity.AdministrativeWard;
import com.stylemind.user.repository.AdministrativeProvinceRepository;
import com.stylemind.user.repository.AdministrativeWardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdministrativeDataImporter {

    private static final String DATA_VERSION = "v4.0.0";
    private static final String RESOURCE = "data/vietnam-admin-units-v4.0.0.json";

    private final ObjectMapper objectMapper;
    private final AdministrativeProvinceRepository provinceRepository;
    private final AdministrativeWardRepository wardRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void importIfMissing() {
        try (InputStream stream = new ClassPathResource(RESOURCE).getInputStream()) {
            JsonNode provinces = objectMapper.readTree(stream);
            List<AdministrativeProvince> provinceRows = new ArrayList<>();
            List<AdministrativeWard> wardRows = new ArrayList<>();

            for (JsonNode province : provinces) {
                String provinceCode = province.path("Code").asText();
                String provinceName = province.path("FullName").asText();
                provinceRows.add(AdministrativeProvince.builder()
                        .code(provinceCode)
                        .name(provinceName)
                        .type(provinceName.startsWith("Thành phố") ? "MUNICIPALITY" : "PROVINCE")
                        .active(true)
                        .dataVersion(DATA_VERSION)
                        .build());

                for (JsonNode ward : province.path("Wards")) {
                    String wardName = ward.path("FullName").asText();
                    wardRows.add(AdministrativeWard.builder()
                            .code(ward.path("Code").asText())
                            .provinceCode(provinceCode)
                            .name(wardName)
                            .type(resolveWardType(wardName))
                            .active(true)
                            .dataVersion(DATA_VERSION)
                            .build());
                }
            }

            if (provinceRepository.count() == 0) {
                provinceRepository.saveAll(provinceRows);
            }
            if (wardRepository.count() == 0) {
                wardRepository.saveAll(wardRows);
            }
            log.info("Administrative dataset ready: provinces={}, wards={}, version={}",
                    provinceRows.size(), wardRows.size(), DATA_VERSION);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to load bundled administrative dataset", ex);
        }
    }

    private String resolveWardType(String name) {
        if (name.startsWith("Phường")) return "WARD";
        if (name.startsWith("Đặc khu")) return "SPECIAL_ZONE";
        return "COMMUNE";
    }
}
