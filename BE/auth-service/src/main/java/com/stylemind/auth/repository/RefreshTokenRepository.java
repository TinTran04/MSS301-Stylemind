package com.stylemind.auth.repository;

import com.stylemind.auth.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    List<RefreshToken> findByAccountId(UUID accountId);

    List<RefreshToken> findByAccountIdAndRevokedAtIsNull(UUID accountId);

    List<RefreshToken> findByExpiresAtBefore(LocalDateTime expiresAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
