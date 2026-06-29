package com.stylemind.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stylemind.auth.dto.AuthResponse;
import com.stylemind.auth.dto.ChangePasswordRequest;
import com.stylemind.auth.dto.LoginRequest;
import com.stylemind.auth.entity.Account;
import com.stylemind.auth.entity.AccountRole;
import com.stylemind.auth.entity.AccountStatus;
import com.stylemind.auth.entity.RefreshToken;
import com.stylemind.auth.repository.AccountRepository;
import com.stylemind.auth.repository.OutboxEventRepository;
import com.stylemind.auth.repository.RefreshTokenRepository;
import com.stylemind.common.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthRefreshTokenLifecycleTest {

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

    private Account account;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        outboxEventRepository.deleteAll();
        accountRepository.deleteAll();
        account = saveAccount("customer@example.com");
    }

    @Test
    void refreshRotatesRefreshTokenAndIssuesNewAccessToken() {
        AuthResponse.LoginResponse login = login();

        AuthResponse.LoginResponse refreshed = authService.refresh(login.getRefreshToken());

        assertThat(refreshed.getAccessToken()).isNotBlank();
        assertThat(refreshed.getAccessToken()).isNotEqualTo(login.getAccessToken());
        assertThat(refreshed.getRefreshToken()).isNotBlank();
        assertThat(refreshed.getRefreshToken()).isNotEqualTo(login.getRefreshToken());
        assertThat(activeTokens()).hasSize(1);
        assertThat(allTokens()).hasSize(2);
        assertThat(allTokens()).filteredOn(token -> token.getRevokedAt() != null).hasSize(1);
    }

    @Test
    void refreshRejectsExpiredToken() {
        AuthResponse.LoginResponse login = login();
        RefreshToken token = onlyToken();
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        refreshTokenRepository.saveAndFlush(token);

        assertInvalidRefreshToken(() -> authService.refresh(login.getRefreshToken()));
        assertThat(onlyToken().getRevokedAt()).isNotNull();
    }

    @Test
    void refreshRejectsRevokedTokenAndRevokesRelatedActiveTokens() {
        AuthResponse.LoginResponse firstLogin = login();
        RefreshToken revoked = onlyToken();
        revoked.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.saveAndFlush(revoked);
        AuthResponse.LoginResponse secondLogin = login();

        assertInvalidRefreshToken(() -> authService.refresh(firstLogin.getRefreshToken()));

        assertThat(activeTokens()).isEmpty();
        assertThat(secondLogin.getRefreshToken()).isNotBlank();
    }

    @Test
    void oldRefreshTokenCannotBeUsedAfterRotation() {
        AuthResponse.LoginResponse login = login();
        authService.refresh(login.getRefreshToken());

        assertInvalidRefreshToken(() -> authService.refresh(login.getRefreshToken()));
        assertThat(activeTokens()).isEmpty();
    }

    @Test
    void concurrentRefreshDoesNotCreateTwoActiveTokens() throws Exception {
        AuthResponse.LoginResponse login = login();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<Boolean> first = executor.submit(() -> refreshConcurrently(start, login.getRefreshToken()));
        Future<Boolean> second = executor.submit(() -> refreshConcurrently(start, login.getRefreshToken()));

        start.countDown();

        boolean firstSucceeded = first.get(10, TimeUnit.SECONDS);
        boolean secondSucceeded = second.get(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertThat(List.of(firstSucceeded, secondSucceeded)).containsExactlyInAnyOrder(true, false);
        assertThat(activeTokens()).hasSizeLessThanOrEqualTo(1);
    }

    @Test
    void logoutRevokesCurrentRefreshToken() {
        AuthResponse.LoginResponse login = login();

        authService.logout(login.getRefreshToken());

        assertThat(onlyToken().getRevokedAt()).isNotNull();
    }

    @Test
    void logoutAllRevokesAllActiveRefreshTokens() {
        login();
        login();

        authService.logoutAll(account.getId().toString());

        assertThat(activeTokens()).isEmpty();
    }

    @Test
    void changePasswordUpdatesPasswordIncrementsTokenVersionAndRevokesTokens() {
        AuthResponse.LoginResponse login = login();

        authService.changePassword(account.getId().toString(), ChangePasswordRequest.builder()
                .currentPassword("StrongPassword123!")
                .newPassword("NewStrongPassword123!")
                .build());

        Account updated = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewStrongPassword123!", updated.getPasswordHash())).isTrue();
        assertThat(updated.getTokenVersion()).isEqualTo(1);
        assertThat(activeTokens()).isEmpty();
        assertInvalidRefreshToken(() -> authService.refresh(login.getRefreshToken()));
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        login();

        assertThatThrownBy(() -> authService.changePassword(account.getId().toString(), ChangePasswordRequest.builder()
                .currentPassword("WrongPassword123!")
                .newPassword("NewStrongPassword123!")
                .build()))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("INVALID_CREDENTIALS");
                    assertThat(ex.getHttpStatus()).isEqualTo(401);
                });

        Account unchanged = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(unchanged.getTokenVersion()).isZero();
        assertThat(activeTokens()).hasSize(1);
    }

    private boolean refreshConcurrently(CountDownLatch start, String refreshToken) throws Exception {
        start.await();
        try {
            authService.refresh(refreshToken);
            return true;
        } catch (BusinessException ex) {
            assertThat(ex.getErrorCode()).isEqualTo("INVALID_REFRESH_TOKEN");
            return false;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private void assertInvalidRefreshToken(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("INVALID_REFRESH_TOKEN");
                    assertThat(ex.getHttpStatus()).isEqualTo(401);
                });
    }

    private AuthResponse.LoginResponse login() {
        LoginRequest request = new LoginRequest();
        request.setEmail(account.getEmail());
        request.setPassword("StrongPassword123!");
        return authService.login(request);
    }

    private RefreshToken onlyToken() {
        return refreshTokenRepository.findByAccountId(account.getId()).get(0);
    }

    private List<RefreshToken> allTokens() {
        return refreshTokenRepository.findByAccountId(account.getId());
    }

    private List<RefreshToken> activeTokens() {
        return refreshTokenRepository.findByAccountIdAndRevokedAtIsNull(account.getId());
    }

    private Account saveAccount(String email) {
        return accountRepository.saveAndFlush(Account.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash(passwordEncoder.encode("StrongPassword123!"))
                .role(AccountRole.CUSTOMER)
                .status(AccountStatus.ACTIVE)
                .emailVerified(true)
                .tokenVersion(0)
                .build());
    }
}
