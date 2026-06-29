package com.stylemind.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stylemind.auth.entity.Account;
import com.stylemind.auth.entity.AccountRole;
import com.stylemind.auth.entity.AccountStatus;
import com.stylemind.auth.entity.RefreshToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AccountPersistenceTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void enforcesUniqueAccountEmail() {
        accountRepository.saveAndFlush(account("customer@example.com"));

        assertThatThrownBy(() -> accountRepository.saveAndFlush(account("customer@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void persistsRefreshTokenWithAccountRelationship() {
        Account account = accountRepository.saveAndFlush(account("token-owner@example.com"));

        RefreshToken refreshToken = RefreshToken.builder()
                .account(account)
                .tokenHash("sha256-token-hash")
                .deviceName("Chrome on Windows")
                .ipAddress("127.0.0.1")
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        refreshTokenRepository.saveAndFlush(refreshToken);

        List<RefreshToken> tokens = refreshTokenRepository.findByAccountId(account.getId());

        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getAccount().getId()).isEqualTo(account.getId());
        assertThat(tokens.get(0).getTokenHash()).isEqualTo("sha256-token-hash");
    }

    private Account account(String email) {
        return Account.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("$2a$12$hashed-password-value")
                .role(AccountRole.CUSTOMER)
                .status(AccountStatus.ACTIVE)
                .emailVerified(false)
                .tokenVersion(0)
                .build();
    }
}
