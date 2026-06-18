package com.stylemind.user.repository;

import com.stylemind.user.entity.CustomerStyleProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerStyleProfileRepository extends JpaRepository<CustomerStyleProfile, String> {
    Optional<CustomerStyleProfile> findByUserId(String userId);
}