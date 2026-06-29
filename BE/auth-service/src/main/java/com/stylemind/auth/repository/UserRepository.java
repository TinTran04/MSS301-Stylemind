package com.stylemind.auth.repository;

import com.stylemind.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    boolean existsByEmail(String email);

    // CAST(:search AS string) forces Hibernate to bind the nullable param as VARCHAR,
    // avoiding the "lower(bytea) does not exist" error when search is null.
    @Query("SELECT u FROM User u WHERE " +
           "(CAST(:search AS string) IS NULL " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
           "AND (CAST(:role AS string) IS NULL OR u.role = :role) " +
           "AND (:enabled IS NULL OR u.enabled = :enabled)")
    Page<User> findAllWithSearch(
            @Param("search") String search,
            @Param("role") String role,
            @Param("enabled") Boolean enabled,
            Pageable pageable);
}