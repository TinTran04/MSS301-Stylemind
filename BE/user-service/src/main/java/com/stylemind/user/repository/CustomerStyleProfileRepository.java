package com.stylemind.user.repository;

import com.stylemind.user.entity.CustomerStyleProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerStyleProfileRepository extends JpaRepository<CustomerStyleProfile, String> {
    @Modifying
    @Query(value = """
            INSERT INTO customer_style_profiles (user_id, created_at, updated_at)
            VALUES (:userId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (user_id) DO NOTHING
            """, nativeQuery = true)
    int insertProfileShell(@Param("userId") String userId);
}
