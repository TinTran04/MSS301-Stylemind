package com.stylemind.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.stylemind.auth.dto.AuthResponse;
import com.stylemind.auth.dto.ForgotPasswordRequest;
import com.stylemind.auth.dto.LoginRequest;
import com.stylemind.auth.dto.RegisterRequest;
import com.stylemind.auth.dto.ResetPasswordRequest;
import com.stylemind.auth.dto.VerifyEmailRequest;
import com.stylemind.auth.entity.Account;
import com.stylemind.auth.entity.AccountRole;
import com.stylemind.auth.entity.AccountStatus;
import com.stylemind.auth.entity.EmailVerificationToken;
import com.stylemind.auth.entity.PasswordResetToken;
import com.stylemind.auth.repository.AccountRepository;
import com.stylemind.auth.repository.EmailVerificationTokenRepository;
import com.stylemind.auth.repository.OutboxEventRepository;
import com.stylemind.auth.repository.PasswordResetTokenRepository;
import com.stylemind.auth.repository.RefreshTokenRepository;
import com.stylemind.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthEmailRecoveryIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailSender emailSender;

    @BeforeEach
    void cleanDatabase() {
        passwordResetTokenRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        outboxEventRepository.deleteAll();
        accountRepository.deleteAll();
        reset(emailSender);
    }

    @Test
    void verifyEmailSucceedsAndMarksTokenUsed() {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("customer@example.com")
                .password("StrongPassword123!")
                .fullName("Nguyen Van A")
                .build();
        authService.register(registerRequest);
        String rawToken = capturedVerificationToken();

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken(rawToken);
        authService.verifyEmail(request);

        Account account = accountRepository.findByEmail("customer@example.com").orElseThrow();
        assertThat(account.isEmailVerified()).isTrue();
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(emailVerificationTokenRepository.findByTokenHash(hashToken(rawToken)).orElseThrow().getUsedAt()).isNotNull();
    }

    @Test
    void verifyEmailRejectsWrongToken() {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken("wrong-token");

        assertInvalidEmailToken(() -> authService.verifyEmail(request));
    }

    @Test
    void verifyEmailRejectsExpiredToken() {
        Account account = saveAccount("pending@example.com", AccountStatus.PENDING, false);
        String rawToken = "expired-email-token";
        emailVerificationTokenRepository.saveAndFlush(EmailVerificationToken.builder()
                .account(account)
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build());

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken(rawToken);

        assertInvalidEmailToken(() -> authService.verifyEmail(request));
    }

    @Test
    void verifyEmailRejectsUsedToken() {
        Account account = saveAccount("used@example.com", AccountStatus.PENDING, false);
        String rawToken = "used-email-token";
        emailVerificationTokenRepository.saveAndFlush(EmailVerificationToken.builder()
                .account(account)
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .usedAt(LocalDateTime.now())
                .build());

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken(rawToken);

        assertInvalidEmailToken(() -> authService.verifyEmail(request));
    }

    @Test
    void forgotPasswordDoesNotRevealAccountExistence() {
        ForgotPasswordRequest missingRequest = new ForgotPasswordRequest();
        missingRequest.setEmail("missing@example.com");

        authService.forgotPassword(missingRequest);

        verify(emailSender, never()).sendPasswordReset(any(Account.class), anyString());
        assertThat(passwordResetTokenRepository.findAll()).isEmpty();
    }

    @Test
    void resetPasswordSucceedsRevokesRefreshTokensAndInvalidatesResetToken() {
        Account account = saveAccount("reset@example.com", AccountStatus.ACTIVE, true);
        AuthResponse.LoginResponse login = login(account.getEmail(), "StrongPassword123!");
        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest();
        forgotRequest.setEmail(account.getEmail());
        authService.forgotPassword(forgotRequest);
        String rawResetToken = capturedPasswordResetToken();

        ResetPasswordRequest resetRequest = new ResetPasswordRequest();
        resetRequest.setToken(rawResetToken);
        resetRequest.setNewPassword("NewStrongPassword123!");
        authService.resetPassword(resetRequest);

        Account updated = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewStrongPassword123!", updated.getPasswordHash())).isTrue();
        assertThat(updated.getTokenVersion()).isEqualTo(1);
        assertThat(refreshTokenRepository.findByAccountIdAndRevokedAtIsNull(account.getId())).isEmpty();
        assertThat(passwordResetTokenRepository.findByTokenHash(hashToken(rawResetToken)).orElseThrow().getUsedAt()).isNotNull();
        assertInvalidRefreshToken(() -> authService.refresh(login.getRefreshToken()));
    }

    @Test
    void resetPasswordRejectsUsedToken() {
        Account account = saveAccount("reuse@example.com", AccountStatus.ACTIVE, true);
        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest();
        forgotRequest.setEmail(account.getEmail());
        authService.forgotPassword(forgotRequest);
        String rawResetToken = capturedPasswordResetToken();

        ResetPasswordRequest resetRequest = new ResetPasswordRequest();
        resetRequest.setToken(rawResetToken);
        resetRequest.setNewPassword("NewStrongPassword123!");
        authService.resetPassword(resetRequest);

        assertInvalidPasswordResetToken(() -> authService.resetPassword(resetRequest));
    }

    private String capturedVerificationToken() {
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendEmailVerification(any(Account.class), tokenCaptor.capture());
        return tokenCaptor.getValue();
    }

    private String capturedPasswordResetToken() {
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendPasswordReset(any(Account.class), tokenCaptor.capture());
        return tokenCaptor.getValue();
    }

    private AuthResponse.LoginResponse login(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return authService.login(request);
    }

    private Account saveAccount(String email, AccountStatus status, boolean emailVerified) {
        return accountRepository.saveAndFlush(Account.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash(passwordEncoder.encode("StrongPassword123!"))
                .role(AccountRole.CUSTOMER)
                .status(status)
                .emailVerified(emailVerified)
                .tokenVersion(0)
                .build());
    }

    private void assertInvalidEmailToken(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("AUTH_EMAIL_VERIFICATION_TOKEN_INVALID");
                    assertThat(ex.getHttpStatus()).isEqualTo(400);
                });
    }

    private void assertInvalidPasswordResetToken(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("AUTH_PASSWORD_RESET_TOKEN_INVALID");
                    assertThat(ex.getHttpStatus()).isEqualTo(400);
                });
    }

    private void assertInvalidRefreshToken(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("INVALID_REFRESH_TOKEN");
                    assertThat(ex.getHttpStatus()).isEqualTo(401);
                });
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }
}
