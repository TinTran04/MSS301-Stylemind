package com.stylemind.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stylemind.auth.dto.AuthResponse;
import com.stylemind.auth.dto.LoginRequest;
import com.stylemind.auth.entity.Account;
import com.stylemind.auth.entity.AccountRole;
import com.stylemind.auth.entity.AccountStatus;
import com.stylemind.auth.entity.RefreshToken;
import com.stylemind.auth.repository.AccountRepository;
import com.stylemind.auth.repository.OutboxEventRepository;
import com.stylemind.auth.repository.RefreshTokenRepository;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.security.JwtUtil;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthLoginIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        outboxEventRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void loginReturnsAccessTokenAndStoresOnlyRefreshTokenHash() {
        Account account = saveAccount("customer@example.com", AccountStatus.ACTIVE);

        AuthResponse.LoginResponse response = authService.login(loginRequest(" Customer@Example.COM ", "StrongPassword123!"));

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresInSeconds()).isEqualTo(900);
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getRefreshExpiresInSeconds()).isEqualTo(604800);
        assertThat(response.getUser().getId()).isEqualTo(account.getId().toString());

        List<RefreshToken> refreshTokens = refreshTokenRepository.findByAccountId(account.getId());
        assertThat(refreshTokens).hasSize(1);
        assertThat(refreshTokens.get(0).getTokenHash()).isNotEqualTo(response.getRefreshToken());
        assertThat(refreshTokens.get(0).getTokenHash()).hasSize(64);
    }

    @Test
    void loginWithUnknownEmailReturnsInvalidCredentials() {
        assertInvalidCredentials(() -> authService.login(loginRequest("missing@example.com", "StrongPassword123!")));
    }

    @Test
    void loginWithWrongPasswordReturnsInvalidCredentials() {
        saveAccount("customer@example.com", AccountStatus.ACTIVE);

        assertInvalidCredentials(() -> authService.login(loginRequest("customer@example.com", "WrongPassword123!")));
    }

    @Test
    void loginRejectsLockedAccount() {
        saveAccount("locked@example.com", AccountStatus.LOCKED);

        assertThatThrownBy(() -> authService.login(loginRequest("locked@example.com", "StrongPassword123!")))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("ACCOUNT_LOCKED");
                    assertThat(ex.getHttpStatus()).isEqualTo(403);
                });
    }

    @Test
    void loginRejectsDisabledAccount() {
        saveAccount("disabled@example.com", AccountStatus.DISABLED);

        assertThatThrownBy(() -> authService.login(loginRequest("disabled@example.com", "StrongPassword123!")))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("ACCOUNT_DISABLED");
                    assertThat(ex.getHttpStatus()).isEqualTo(403);
                });
    }

    @Test
    void loginRejectsPendingAccountWhenEmailVerificationIsRequired() {
        saveAccount("pending@example.com", AccountStatus.PENDING);

        assertThatThrownBy(() -> authService.login(loginRequest("pending@example.com", "StrongPassword123!")))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("ACCOUNT_PENDING_EMAIL_VERIFICATION");
                    assertThat(ex.getHttpStatus()).isEqualTo(403);
                });
    }

    @Test
    void accessTokenContainsOnlyExpectedAuthClaims() {
        Account account = saveAccount("claims@example.com", AccountStatus.ACTIVE);

        AuthResponse.LoginResponse response = authService.login(loginRequest("claims@example.com", "StrongPassword123!"));
        Claims claims = jwtUtil.extractClaim(response.getAccessToken(), value -> value);

        assertThat(claims.getSubject()).isEqualTo(account.getId().toString());
        assertThat(claims.get("role", String.class)).isEqualTo("CUSTOMER");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(claims.get("jti", String.class)).isNotBlank();
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims).doesNotContainKeys("email", "fullName", "phone", "address", "password", "password_hash");
    }

    @Test
    void accessTokenExpirationFollowsConfiguredTtl() {
        saveAccount("ttl@example.com", AccountStatus.ACTIVE);

        AuthResponse.LoginResponse response = authService.login(loginRequest("ttl@example.com", "StrongPassword123!"));
        Claims claims = jwtUtil.extractClaim(response.getAccessToken(), value -> value);
        long ttlSeconds = Duration.between(
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant()
        ).toSeconds();

        assertThat(ttlSeconds).isBetween(899L, 900L);
    }

    @Test
    void serializedLoginResponseDoesNotExposePasswordOrRefreshTokenHash() throws Exception {
        saveAccount("safe-response@example.com", AccountStatus.ACTIVE);

        AuthResponse.LoginResponse response = authService.login(loginRequest("safe-response@example.com", "StrongPassword123!"));
        String json = objectMapper.writeValueAsString(response);

        assertThat(json).doesNotContain("password", "passwordHash", "password_hash", "tokenHash", "refreshToken");
    }

    private void assertInvalidCredentials(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("INVALID_CREDENTIALS");
                    assertThat(ex.getHttpStatus()).isEqualTo(401);
                });
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private Account saveAccount(String email, AccountStatus status) {
        return accountRepository.saveAndFlush(Account.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash(passwordEncoder.encode("StrongPassword123!"))
                .role(AccountRole.CUSTOMER)
                .status(status)
                .emailVerified(status == AccountStatus.ACTIVE)
                .tokenVersion(0)
                .build());
    }
}
