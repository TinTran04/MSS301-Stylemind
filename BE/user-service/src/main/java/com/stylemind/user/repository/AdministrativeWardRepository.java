package com.stylemind.user.repository;

import com.stylemind.user.entity.AdministrativeWard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdministrativeWardRepository extends JpaRepository<AdministrativeWard, String> {
    List<AdministrativeWard> findByProvinceCodeAndActiveTrueOrderByNameAsc(String provinceCode);
}
