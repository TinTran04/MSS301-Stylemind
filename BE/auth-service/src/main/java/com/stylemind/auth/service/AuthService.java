package com.stylemind.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stylemind.auth.dto.AuthResponse;
import com.stylemind.auth.dto.ChangePasswordRequest;
import com.stylemind.auth.dto.ForgotPasswordRequest;
import com.stylemind.auth.dto.LoginRequest;
import com.stylemind.auth.dto.RegisterRequest;
import com.stylemind.auth.dto.RegisterResponse;
import com.stylemind.auth.dto.ResetPasswordRequest;
import com.stylemind.auth.dto.AccountStatusResponse;
import com.stylemind.auth.dto.RoleResponse;
import com.stylemind.auth.dto.UpdateAccountStatusRequest;
import com.stylemind.auth.dto.UpdateRoleRequest;
import com.stylemind.auth.dto.UserResponse;
import com.stylemind.auth.dto.VerifyEmailRequest;
import com.stylemind.auth.entity.Account;
import com.stylemind.auth.entity.AccountRole;
import com.stylemind.auth.entity.AccountStatus;
import com.stylemind.auth.entity.EmailVerificationToken;
import com.stylemind.auth.entity.OutboxEvent;
import com.stylemind.auth.entity.OutboxEventStatus;
import com.stylemind.auth.entity.PasswordResetToken;
import com.stylemind.auth.entity.RefreshToken;
import com.stylemind.auth.repository.AccountRepository;
import com.stylemind.auth.repository.EmailVerificationTokenRepository;
import com.stylemind.auth.repository.OutboxEventRepository;
import com.stylemind.auth.repository.PasswordResetTokenRepository;
import com.stylemind.auth.repository.RefreshTokenRepository;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.security.JwtUtil;
import com.stylemind.common.security.UserPrincipal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final EmailSender emailSender;

    @Value("${auth.password.min-length:8}")
    private int passwordMinLength;

    @Value("${auth.email-verification.required:true}")
    private boolean emailVerificationRequired;

    @Value("${auth.email-verification.token-expiration-ms:86400000}")
    private long emailVerificationTokenExpirationMs;

    @Value("${auth.password-reset.token-expiration-ms:900000}")
    private long passwordResetTokenExpirationMs;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public UserDetails loadUserByUsername(String subject) throws UsernameNotFoundException {
        return findAccountBySubject(subject)
                .map(this::buildUserPrincipal)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }

    public AuthResponse.LoginResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        Account account = accountRepository.findByEmail(normalizedEmail)
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw invalidCredentials();
        }

        validateLoginStatus(account);

        return issueTokenPair(account);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public AuthResponse.LoginResponse refresh(String refreshToken) {
        RefreshToken currentToken = findRefreshToken(refreshToken);
        Account account = currentToken.getAccount();

        if (currentToken.getRevokedAt() != null) {
            revokeAllActiveRefreshTokens(account);
            throw invalidRefreshToken();
        }
        if (currentToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            currentToken.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(currentToken);
            throw invalidRefreshToken();
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw invalidRefreshToken();
        }

        currentToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(currentToken);

        return issueTokenPair(account);
    }

    public void logout(String refreshToken) {
        RefreshToken currentToken = findRefreshToken(refreshToken);
        if (currentToken.getRevokedAt() == null) {
            currentToken.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(currentToken);
        }
    }

    public long logoutAll(String accountId) {
        Account account = accountRepository.findById(UUID.fromString(accountId))
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Account not found", 404));
        long revokedSessions = refreshTokenRepository.findByAccountIdAndRevokedAtIsNull(account.getId()).size();
        revokeAllActiveRefreshTokens(account);
        return revokedSessions;
    }

    public void changePassword(String accountId, ChangePasswordRequest request) {
        Account account = accountRepository.findById(UUID.fromString(accountId))
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Account not found", 404));

        if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPasswordHash())) {
            throw invalidCredentials();
        }
        validatePassword(request.getNewPassword());
        if (passwordEncoder.matches(request.getNewPassword(), account.getPasswordHash())) {
            throw new BusinessException("PASSWORD_REUSED", "New password must be different from current password", 400);
        }

        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        account.setTokenVersion(account.getTokenVersion() + 1);
        accountRepository.save(account);
        revokeAllActiveRefreshTokens(account);
    }

    public AccountStatusResponse updateAccountStatus(String accountId, UpdateAccountStatusRequest request) {
        Account account = findAccountById(accountId);
        account.setStatus(request.getAccountStatus());
        accountRepository.save(account);
        if (request.getAccountStatus() != AccountStatus.ACTIVE) {
            revokeAllActiveRefreshTokens(account);
        }
        return AccountStatusResponse.builder()
                .userId(account.getId().toString())
                .accountStatus(account.getStatus().name())
                .build();
    }

    public RoleResponse updateAccountRole(String accountId, UpdateRoleRequest request) {
        Account account = findAccountById(accountId);
        account.setRole(request.getRole());
        account.setTokenVersion(account.getTokenVersion() + 1);
        accountRepository.save(account);
        revokeAllActiveRefreshTokens(account);
        return RoleResponse.builder()
                .userId(account.getId().toString())
                .role(account.getRole().name())
                .build();
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        accountRepository.findByEmail(normalizedEmail).ifPresent(account -> {
            String resetToken = createPasswordResetToken(account);
            emailSender.sendPasswordReset(account, resetToken);
        });
    }

    public void resetPassword(ResetPasswordRequest request) {
        validatePassword(request.getNewPassword());

        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hashToken(request.getToken()))
                .orElseThrow(this::invalidPasswordResetToken);

        LocalDateTime now = LocalDateTime.now();
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(now)) {
            throw invalidPasswordResetToken();
        }

        Account account = token.getAccount();
        token.setUsedAt(now);
        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        account.setTokenVersion(account.getTokenVersion() + 1);

        passwordResetTokenRepository.save(token);
        accountRepository.save(account);
        revokeAllActiveRefreshTokens(account);
    }

    public AccountStatus verifyEmail(VerifyEmailRequest request) {
        EmailVerificationToken token = emailVerificationTokenRepository.findByTokenHash(hashToken(request.getToken()))
                .orElseThrow(this::invalidEmailVerificationToken);

        LocalDateTime now = LocalDateTime.now();
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(now)) {
            throw invalidEmailVerificationToken();
        }

        Account account = token.getAccount();
        token.setUsedAt(now);
        account.setEmailVerified(true);
        if (account.getStatus() == AccountStatus.PENDING) {
            account.setStatus(AccountStatus.ACTIVE);
        }

        emailVerificationTokenRepository.save(token);
        accountRepository.save(account);
        return account.getStatus();
    }

    private AuthResponse.LoginResponse issueTokenPair(Account account) {
        UserPrincipal principal = buildUserPrincipal(account);
        String accessToken = jwtUtil.generateAccessToken(
                principal,
                account.getId().toString(),
                account.getRole().name()
        );
        String refreshToken = generateRefreshTokenValue();

        refreshTokenRepository.save(RefreshToken.builder()
                .account(account)
                .tokenHash(hashToken(refreshToken))
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(jwtUtil.getRefreshTokenExpiration())))
                .build());

        return AuthResponse.LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresInSeconds(jwtUtil.getAccessTokenExpiration() / 1000)
                .refreshToken(refreshToken)
                .refreshExpiresInSeconds(jwtUtil.getRefreshTokenExpiration() / 1000)
                .user(buildUserResponse(account))
                .build();
    }

    private RefreshToken findRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw invalidRefreshToken();
        }
        return refreshTokenRepository.findByTokenHash(hashToken(refreshToken))
                .orElseThrow(this::invalidRefreshToken);
    }

    private void revokeAllActiveRefreshTokens(Account account) {
        LocalDateTime revokedAt = LocalDateTime.now();
        refreshTokenRepository.findByAccountIdAndRevokedAtIsNull(account.getId())
                .forEach(token -> token.setRevokedAt(revokedAt));
    }

    private void validateLoginStatus(Account account) {
        if (account.getStatus() == AccountStatus.LOCKED) {
            throw new BusinessException("ACCOUNT_LOCKED", "Account is locked", 403);
        }
        if (account.getStatus() == AccountStatus.DISABLED) {
            throw new BusinessException("ACCOUNT_DISABLED", "Account is disabled", 403);
        }
        if (emailVerificationRequired && account.getStatus() == AccountStatus.PENDING) {
            throw new BusinessException("ACCOUNT_PENDING_EMAIL_VERIFICATION", "Account email is not verified", 403);
        }
    }

    private String generateRefreshTokenValue() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateOneTimeTokenValue() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    private BusinessException invalidCredentials() {
        return new BusinessException("INVALID_CREDENTIALS", "Invalid email or password", 401);
    }

    private BusinessException invalidRefreshToken() {
        return new BusinessException("INVALID_REFRESH_TOKEN", "Invalid refresh token", 401);
    }

    private BusinessException invalidEmailVerificationToken() {
        return new BusinessException(
                "AUTH_EMAIL_VERIFICATION_TOKEN_INVALID",
                "Email verification token is invalid",
                400
        );
    }

    private BusinessException invalidPasswordResetToken() {
        return new BusinessException(
                "AUTH_PASSWORD_RESET_TOKEN_INVALID",
                "Password reset token is invalid",
                400
        );
    }

    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        validatePassword(request.getPassword());

        if (accountRepository.existsByEmail(normalizedEmail)) {
            throw emailAlreadyExists();
        }

        Account account = Account.builder()
                .id(UUID.randomUUID())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(AccountRole.CUSTOMER)
                .status(AccountStatus.PENDING)
                .emailVerified(false)
                .tokenVersion(0)
                .build();

        try {
            account = accountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException ex) {
            throw emailAlreadyExists();
        }

        outboxEventRepository.save(buildUserRegisteredEvent(account, request.getFullName()));
        String verificationToken = createEmailVerificationToken(account);
        emailSender.sendEmailVerification(account, verificationToken);

        return buildRegisterResponse(account);
    }

    public UserResponse getCurrentUser(String userId) {
        Account account = accountRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Account not found", 404));
        return buildUserResponse(account);
    }

    private UserPrincipal buildUserPrincipal(Account account) {
        return new UserPrincipal(
                account.getId().toString(),
                account.getEmail(),
                account.getPasswordHash(),
                account.getRole().name(),
                "LOCAL",
                account.isEnabled()
        );
    }

    private UserResponse buildUserResponse(Account account) {
        return UserResponse.builder()
                .id(account.getId().toString())
                .email(account.getEmail())
                .role(account.getRole().name())
                .provider("LOCAL")
                .createdAt(account.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .build();
    }

    private RegisterResponse buildRegisterResponse(Account account) {
        return RegisterResponse.builder()
                .id(account.getId().toString())
                .email(account.getEmail())
                .role(account.getRole().name())
                .status(account.getStatus().name())
                .emailVerified(account.isEmailVerified())
                .createdAt(account.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .build();
    }

    private OutboxEvent buildUserRegisteredEvent(Account account, String fullName) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        Map<String, Object> payload = Map.of(
                "eventId", eventId.toString(),
                "eventType", "USER_REGISTERED",
                "occurredAt", occurredAt.toString(),
                "data", Map.of(
                        "userId", account.getId().toString(),
                        "fullName", fullName.trim()
                )
        );

        return OutboxEvent.builder()
                .id(eventId)
                .aggregateId(account.getId())
                .eventType("USER_REGISTERED")
                .payload(writePayload(payload))
                .status(OutboxEventStatus.PENDING)
                .build();
    }

    private String createEmailVerificationToken(Account account) {
        String rawToken = generateOneTimeTokenValue();
        emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                .account(account)
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(emailVerificationTokenExpirationMs)))
                .build());
        return rawToken;
    }

    private String createPasswordResetToken(Account account) {
        String rawToken = generateOneTimeTokenValue();
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .account(account)
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(passwordResetTokenExpirationMs)))
                .build());
        return rawToken;
    }

    private String writePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize outbox payload", ex);
        }
    }

    private void validatePassword(String password) {
        if (password.length() < passwordMinLength || password.length() > 128) {
            throw new BusinessException(
                    "INVALID_PASSWORD",
                    "Password must be between " + passwordMinLength + " and 128 characters",
                    400
            );
        }
    }

    private BusinessException emailAlreadyExists() {
        return new BusinessException("EMAIL_ALREADY_EXISTS", "Email is already in use", 409);
    }

    private Account findAccountById(String accountId) {
        try {
            return accountRepository.findById(UUID.fromString(accountId))
                    .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Account not found", 404));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("USER_NOT_FOUND", "Account not found", 404);
        }
    }

    private java.util.Optional<Account> findAccountBySubject(String subject) {
        try {
            return accountRepository.findById(UUID.fromString(subject));
        } catch (IllegalArgumentException ex) {
            return accountRepository.findByEmail(normalizeEmail(subject));
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
