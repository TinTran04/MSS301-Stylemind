package com.stylemind.auth.repository;

import com.stylemind.auth.entity.PasswordResetToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    List<PasswordResetToken> findByAccountId(UUID accountId);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findByExpiresAtBefore(LocalDateTime expiresAt);
}
