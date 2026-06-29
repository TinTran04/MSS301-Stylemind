package com.stylemind.user.repository;

import com.stylemind.user.entity.UserProfile;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Page<UserProfile> findByFullNameContainingIgnoreCase(String fullName, Pageable pageable);
}
