package com.stylemind.auth.repository;

import com.stylemind.auth.entity.EmailVerificationToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    List<EmailVerificationToken> findByAccountId(UUID accountId);

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    List<EmailVerificationToken> findByExpiresAtBefore(LocalDateTime expiresAt);
}
