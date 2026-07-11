package com.stylemind.auth.service;

import com.stylemind.auth.dto.AdminCreateUserRequest;
import com.stylemind.auth.dto.AdminUserResponse;
import com.stylemind.auth.dto.AdminUserSummaryResponse;
import com.stylemind.auth.dto.AuthResponse;
import com.stylemind.auth.dto.ForgotPasswordRequest;
import com.stylemind.auth.dto.ForgotPasswordVerifyResponse;
import com.stylemind.auth.dto.InternalEmailNotificationRequest;
import com.stylemind.auth.dto.LoginRequest;
import com.stylemind.auth.dto.PasswordSetupRequest;
import com.stylemind.auth.dto.RegisterRequest;
import com.stylemind.auth.dto.ResendRegisterOtpRequest;
import com.stylemind.auth.dto.ResetForgotPasswordRequest;
import com.stylemind.auth.dto.UserResponse;
import com.stylemind.auth.dto.VerifyForgotPasswordOtpRequest;
import com.stylemind.auth.dto.VerifyRegisterOtpRequest;
import com.stylemind.auth.entity.PendingRegistration;
import com.stylemind.auth.entity.User;
import com.stylemind.auth.entity.AccountStatus;
import com.stylemind.auth.entity.AuditLog;
import com.stylemind.auth.feign.NotificationInternalClient;
import com.stylemind.auth.mapper.AuthMapper;
import com.stylemind.auth.repository.AuditLogRepository;
import com.stylemind.auth.repository.PendingRegistrationRepository;
import com.stylemind.auth.repository.UserRepository;
import com.stylemind.common.dto.PageResponse;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.security.JwtUtil;
import com.stylemind.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService implements UserDetailsService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ObjectProvider<AuthenticationManager> authenticationManagerProvider;
    private final NotificationInternalClient notificationInternalClient;
    private final AuthMapper authMapper;
    private final AuditLogRepository auditLogRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${app.auth.setup-token-expiry-minutes:1440}")
    private long setupTokenExpiryMinutes;

    @Value("${app.auth.reset-otp-expiry-minutes:10}")
    private long resetOtpExpiryMinutes;

    @Value("${app.auth.reset-token-expiry-minutes:30}")
    private long resetTokenExpiryMinutes;

    @Value("${app.auth.reset-otp-max-attempts:5}")
    private int resetOtpMaxAttempts;

    @Value("${app.auth.reset-otp-resend-cooldown-seconds:60}")
    private long resetOtpResendCooldownSeconds;

    @Value("${app.auth.register-otp-expiry-minutes:10}")
    private long registerOtpExpiryMinutes;

    @Value("${app.auth.register-otp-max-attempts:5}")
    private int registerOtpMaxAttempts;

    @Value("${app.auth.register-otp-resend-cooldown-seconds:60}")
    private long registerOtpResendCooldownSeconds;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(normalizeEmail(email))
                .map(authMapper::toPrincipal)
                .orElseThrow(() -> new UsernameNotFoundException("Authentication failed"));
    }

    public AuthResponse.LoginResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException("AUTH_INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng", 401));

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("AUTH_ACCOUNT_DISABLED", "Tài khoản đã bị khóa", 403);
        }

        if (Boolean.TRUE.equals(user.getPasswordSetupRequired())) {
            throw new BusinessException("AUTH_PASSWORD_SETUP_REQUIRED", "Bạn cần thiết lập mật khẩu từ email mời trước khi đăng nhập", 403);
        }

        try {
            authenticationManagerProvider.getObject().authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new BusinessException("AUTH_INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng", 401);
        }

        String token = jwtUtil.generateAccessToken(
                authMapper.toPrincipal(user),
                user.getId(),
                user.getRole()
        );

        return AuthResponse.LoginResponse.builder()
                .token(token)
                .user(authMapper.toUserResponse(user))
                .build();
    }

    /**
     * AUTH-REG-OTP step 1: begin registration. Validates the email is free,
     * stores the sign-up as a {@link PendingRegistration} (password already
     * hashed), and emails a one-time code. The real {@link User} is NOT created
     * yet — that happens only on {@link #verifyRegistrationOtp}. No account,
     * login or forgot-password state is touched here.
     */
    public void startRegistration(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "Email đã được sử dụng", 400);
        }

        LocalDateTime now = LocalDateTime.now();
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(normalizedEmail).orElse(null);
        if (pending != null && pending.getRequestedAt() != null
                && pending.getRequestedAt().plusSeconds(registerOtpResendCooldownSeconds).isAfter(now)) {
            throw new BusinessException("REGISTER_OTP_COOLDOWN",
                    "Vui lòng đợi một chút trước khi yêu cầu mã OTP mới", 429);
        }

        if (pending == null) {
            pending = PendingRegistration.builder()
                    .id(StringUtil.generateUniqueId())
                    .email(normalizedEmail)
                    .build();
        }
        String otp = generateOtp();
        pending.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        pending.setOtpHash(passwordEncoder.encode(otp));
        pending.setOtpExpiresAt(now.plusMinutes(registerOtpExpiryMinutes));
        pending.setOtpAttempts(0);
        pending.setRequestedAt(now);
        pendingRegistrationRepository.save(pending);

        sendRegisterOtpEmail(normalizedEmail, otp);
    }

    /**
     * AUTH-REG-OTP step 2: verify the emailed code. On success the pending
     * sign-up is promoted to a real ACTIVE {@link User} (reusing the already
     * hashed password) and the pending row is removed. The user then logs in
     * normally via {@link #login}.
     */
    public void verifyRegistrationOtp(VerifyRegisterOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException("REGISTER_OTP_INVALID", "Mã OTP không hợp lệ hoặc đã hết hạn", 400));

        if (pending.getOtpExpiresAt() == null || pending.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("REGISTER_OTP_INVALID", "Mã OTP không hợp lệ hoặc đã hết hạn", 400);
        }

        int attempts = pending.getOtpAttempts() == null ? 0 : pending.getOtpAttempts();
        if (attempts >= registerOtpMaxAttempts) {
            throw new BusinessException("REGISTER_OTP_BLOCKED", "Mã OTP đã bị khóa, vui lòng yêu cầu mã mới", 429);
        }

        if (!passwordEncoder.matches(request.getOtp(), pending.getOtpHash())) {
            pending.setOtpAttempts(attempts + 1);
            pendingRegistrationRepository.save(pending);
            throw new BusinessException("REGISTER_OTP_INVALID", "Mã OTP không hợp lệ hoặc đã hết hạn", 400);
        }

        // Guard against a race where this email got created between start & verify.
        if (userRepository.existsByEmail(normalizedEmail)) {
            pendingRegistrationRepository.delete(pending);
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "Email đã được sử dụng", 400);
        }

        User user = User.builder()
                .id(StringUtil.generateUniqueId())
                .email(normalizedEmail)
                .passwordHash(pending.getPasswordHash()) // already BCrypt-hashed at start
                .provider("LOCAL")
                .role("CUSTOMER")
                .accountStatus(AccountStatus.ACTIVE)
                .passwordSetupRequired(false)
                .build();
        userRepository.save(user);
        pendingRegistrationRepository.delete(pending);
    }

    /**
     * AUTH-REG-OTP: re-issue the registration OTP for a pending sign-up.
     * Enforces the resend cooldown. Stays silent when there is no pending
     * sign-up so the endpoint doesn't reveal registration state.
     */
    public void resendRegistrationOtp(ResendRegisterOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(normalizedEmail).orElse(null);
        if (pending == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (pending.getRequestedAt() != null
                && pending.getRequestedAt().plusSeconds(registerOtpResendCooldownSeconds).isAfter(now)) {
            throw new BusinessException("REGISTER_OTP_COOLDOWN",
                    "Vui lòng đợi một chút trước khi yêu cầu mã OTP mới", 429);
        }

        String otp = generateOtp();
        pending.setOtpHash(passwordEncoder.encode(otp));
        pending.setOtpExpiresAt(now.plusMinutes(registerOtpExpiryMinutes));
        pending.setOtpAttempts(0);
        pending.setRequestedAt(now);
        pendingRegistrationRepository.save(pending);

        sendRegisterOtpEmail(normalizedEmail, otp);
    }

    public UserResponse getCurrentUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng", 404));
        return authMapper.toUserResponse(user);
    }

    public void setupPassword(PasswordSetupRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new BusinessException("AUTH_SETUP_TOKEN_INVALID", "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn", 400));

        if (!Boolean.TRUE.equals(user.getPasswordSetupRequired())
                || user.getPasswordSetupTokenHash() == null
                || user.getPasswordSetupTokenExpiresAt() == null
                || user.getPasswordSetupTokenExpiresAt().isBefore(LocalDateTime.now())
                || !passwordEncoder.matches(request.getToken(), user.getPasswordSetupTokenHash())) {
            throw new BusinessException("AUTH_SETUP_TOKEN_INVALID", "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn", 400);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        clearPasswordSetupState(user);
        clearForgotPasswordState(user);
        userRepository.saveAndFlush(user);
    }

    public void requestForgotPasswordOtp(ForgotPasswordRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(normalizeEmail(request.getEmail()));
        if (userOptional.isEmpty()) {
            return;
        }

        User user = userOptional.get();
        if (!"LOCAL".equalsIgnoreCase(user.getProvider())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (user.getPasswordResetRequestedAt() != null
                && user.getPasswordResetRequestedAt().plusSeconds(resetOtpResendCooldownSeconds).isAfter(now)) {
            return;
        }

        String otp = generateOtp();
        user.setPasswordResetOtpHash(passwordEncoder.encode(otp));
        user.setPasswordResetOtpExpiresAt(now.plusMinutes(resetOtpExpiryMinutes));
        user.setPasswordResetOtpAttempts(0);
        user.setPasswordResetRequestedAt(now);
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetTokenExpiresAt(null);
        userRepository.save(user);

        sendForgotPasswordOtpEmail(user, otp);
    }

    public ForgotPasswordVerifyResponse verifyForgotPasswordOtp(VerifyForgotPasswordOtpRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new BusinessException("AUTH_RESET_OTP_INVALID", "OTP không hợp lệ hoặc đã hết hạn", 400));

        if (user.getPasswordResetOtpHash() == null
                || user.getPasswordResetOtpExpiresAt() == null
                || user.getPasswordResetOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("AUTH_RESET_OTP_INVALID", "OTP không hợp lệ hoặc đã hết hạn", 400);
        }

        int attempts = user.getPasswordResetOtpAttempts() == null ? 0 : user.getPasswordResetOtpAttempts();
        if (attempts >= resetOtpMaxAttempts) {
            throw new BusinessException("AUTH_RESET_OTP_BLOCKED", "OTP đã bị khóa, vui lòng yêu cầu mã mới", 429);
        }

        if (!passwordEncoder.matches(request.getOtp(), user.getPasswordResetOtpHash())) {
            user.setPasswordResetOtpAttempts(attempts + 1);
            userRepository.save(user);
            throw new BusinessException("AUTH_RESET_OTP_INVALID", "OTP không hợp lệ hoặc đã hết hạn", 400);
        }

        String resetToken = UUID.randomUUID() + "-" + UUID.randomUUID();
        user.setPasswordResetTokenHash(passwordEncoder.encode(resetToken));
        user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusMinutes(resetTokenExpiryMinutes));
        user.setPasswordResetOtpHash(null);
        user.setPasswordResetOtpExpiresAt(null);
        user.setPasswordResetOtpAttempts(0);
        userRepository.save(user);

        return ForgotPasswordVerifyResponse.builder()
                .resetToken(resetToken)
                .build();
    }

    public void resetForgotPassword(ResetForgotPasswordRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new BusinessException("AUTH_RESET_TOKEN_INVALID", "Yêu cầu đặt lại mật khẩu không hợp lệ hoặc đã hết hạn", 400));

        if (user.getPasswordResetTokenHash() == null
                || user.getPasswordResetTokenExpiresAt() == null
                || user.getPasswordResetTokenExpiresAt().isBefore(LocalDateTime.now())
                || !passwordEncoder.matches(request.getResetToken(), user.getPasswordResetTokenHash())) {
            throw new BusinessException("AUTH_RESET_TOKEN_INVALID", "Yêu cầu đặt lại mật khẩu không hợp lệ hoặc đã hết hạn", 400);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordSetupRequired(false);
        user.setPasswordSetupTokenHash(null);
        user.setPasswordSetupTokenExpiresAt(null);
        clearForgotPasswordState(user);
        userRepository.saveAndFlush(user);
    }

    // ─── Admin operations ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> listUsers(int page, int size, String search, String role, Boolean enabled) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        AccountStatus accountStatus = enabled == null
                ? null
                : enabled ? AccountStatus.ACTIVE : AccountStatus.DISABLED;
        Page<AdminUserResponse> result = userRepository
                .findAllWithSearch(search, role, accountStatus, pageable)
                .map(authMapper::toAdminUserResponse);
        return PageResponse.of(result);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng", 404));
        return authMapper.toAdminUserResponse(user);
    }

    /** Real user counts for the admin dashboard. No PII or credentials exposed. */
    @Transactional(readOnly = true)
    public AdminUserSummaryResponse getUserSummary() {
        return AdminUserSummaryResponse.builder()
                .totalUsers(userRepository.count())
                .totalCustomers(userRepository.countByRole("CUSTOMER"))
                .totalAdmins(userRepository.countByRole("ADMIN"))
                .build();
    }

    public AdminUserResponse createUserByAdmin(AdminCreateUserRequest request, String requesterId) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "Email đã được sử dụng", 400);
        }

        String setupToken = UUID.randomUUID() + "-" + UUID.randomUUID();

        User user = User.builder()
                .id(StringUtil.generateUniqueId())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .provider("LOCAL")
                .role(request.getRole())
                .accountStatus(AccountStatus.ACTIVE)
                .passwordSetupRequired(true)
                .passwordSetupTokenHash(passwordEncoder.encode(setupToken))
                .passwordSetupTokenExpiresAt(LocalDateTime.now().plusMinutes(setupTokenExpiryMinutes))
                .passwordResetOtpAttempts(0)
                .build();

        user = userRepository.save(user);
        sendSetupPasswordEmail(user, setupToken);
        log.info("Admin {} created user {}", requesterId, user.getId());
        recordAudit(requesterId, "CREATE_ACCOUNT", user.getId(), "role: " + user.getRole());
        return authMapper.toAdminUserResponse(user);
    }

    public AdminUserResponse changeUserRole(String userId, String requesterId, String newRole) {
        if (userId.equals(requesterId)) {
            throw new BusinessException("ADMIN_SELF_ROLE_CHANGE", "Không thể tự thay đổi role của mình", 409);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng", 404));
        String oldRole = user.getRole();
        if ("ADMIN".equals(oldRole) && "CUSTOMER".equals(newRole)) {
            assertNotLastActiveAdmin(user, "Không thể hạ quyền quản trị viên đang hoạt động cuối cùng");
        }
        user.setRole(newRole);
        User saved = userRepository.save(user);
        recordAudit(requesterId, "CHANGE_ROLE", user.getId(), oldRole + " -> " + newRole);
        return authMapper.toAdminUserResponse(saved);
    }

    public AdminUserResponse changeUserEnabled(String userId, String requesterId, Boolean enabled) {
        if (userId.equals(requesterId)) {
            throw new BusinessException("ADMIN_SELF_BAN", "Không thể tự khóa chính mình", 409);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng", 404));
        boolean enable = Boolean.TRUE.equals(enabled);
        if (!enable) {
            assertNotLastActiveAdmin(user, "Không thể khóa quản trị viên đang hoạt động cuối cùng");
        }
        user.setAccountStatus(enable ? AccountStatus.ACTIVE : AccountStatus.DISABLED);
        User saved = userRepository.save(user);
        recordAudit(requesterId, enable ? "ENABLE_ACCOUNT" : "DISABLE_ACCOUNT", user.getId(), null);
        return authMapper.toAdminUserResponse(saved);
    }

    public void deleteUser(String userId, String requesterId) {
        if (userId.equals(requesterId)) {
            throw new BusinessException("ADMIN_SELF_DELETE", "Không thể tự xóa tài khoản của mình", 409);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng", 404));
        assertNotLastActiveAdmin(user, "Không thể xóa quản trị viên đang hoạt động cuối cùng");
        userRepository.delete(user);
        recordAudit(requesterId, "DELETE_ACCOUNT", userId, user.getEmail());
    }

    /** ADM-SELF-04: blocks disable/delete/demote when the target is the sole remaining active ADMIN. */
    private void assertNotLastActiveAdmin(User target, String message) {
        if ("ADMIN".equals(target.getRole()) && target.getAccountStatus() == AccountStatus.ACTIVE) {
            long activeAdmins = userRepository.countByRoleAndAccountStatus("ADMIN", AccountStatus.ACTIVE);
            if (activeAdmins <= 1) {
                throw new BusinessException("ADMIN_LAST_ACTIVE_ADMIN", message, 409);
            }
        }
    }

    /** ADM-SELF-05: audit trail for sensitive admin actions on accounts. */
    private void recordAudit(String actorUserId, String action, String targetUserId, String detail) {
        auditLogRepository.save(AuditLog.builder()
                .id(StringUtil.generateUniqueId())
                .actorUserId(actorUserId)
                .action(action)
                .targetUserId(targetUserId)
                .detail(detail)
                .build());
    }

    private void sendSetupPasswordEmail(User user, String setupToken) {
        String encodedEmail = URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8);
        String encodedToken = URLEncoder.encode(setupToken, StandardCharsets.UTF_8);
        String setupUrl = String.format("%s/reset-password?token=%s&email=%s", frontendBaseUrl, encodedToken, encodedEmail);
        try {
            notificationInternalClient.sendEmail(InternalEmailNotificationRequest.builder()
                    .userId(user.getId())
                    .recipientEmail(user.getEmail())
                    .type("USER_INVITE")
                    .title("Thiết lập mật khẩu StyleMind")
                    .content("Nhấn vào liên kết sau để thiết lập mật khẩu: " + setupUrl)
                    .htmlContent("<p>Xin chào,</p><p>Nhấn vào liên kết sau để thiết lập mật khẩu:</p><p><a href=\"" + setupUrl + "\">Thiết lập mật khẩu</a></p>")
                    .build());
        } catch (Exception ex) {
            log.error("Failed to send setup password email to user {}: {}", user.getId(), ex.getMessage());
            throw new BusinessException("NOTIFICATION_FAILED", "Không thể gửi email thiết lập mật khẩu. Vui lòng thử lại sau.", 503);
        }
    }

    private void sendRegisterOtpEmail(String email, String otp) {
        try {
            notificationInternalClient.sendEmail(InternalEmailNotificationRequest.builder()
                    .recipientEmail(email)
                    .type("REGISTER_OTP")
                    .title("Mã OTP xác thực đăng ký StyleMind")
                    // OTP is placed ONLY in htmlContent; content (which lands in logs/DB)
                    // is redacted, matching the forgot-password OTP email.
                    .content("Mã OTP xác thực đăng ký của bạn là: [PROTECTED]. Mã có hiệu lực trong " + registerOtpExpiryMinutes + " phút.")
                    .htmlContent("<p>Chào mừng bạn đến với StyleMind!</p><p>Mã OTP xác thực đăng ký của bạn là <strong>" + otp + "</strong>.</p><p>Mã có hiệu lực trong " + registerOtpExpiryMinutes + " phút.</p>")
                    .build());
        } catch (Exception ex) {
            // Rethrow so the whole @Transactional start/resend rolls back (no orphan
            // pending row) and the client is told to retry. Never log the OTP/email.
            log.error("Failed to send registration OTP email: {}", ex.getMessage());
            throw new BusinessException("NOTIFICATION_FAILED", "Không thể gửi email xác thực. Vui lòng thử lại sau.", 503);
        }
    }

    private void sendForgotPasswordOtpEmail(User user, String otp) {
        try {
            notificationInternalClient.sendEmail(InternalEmailNotificationRequest.builder()
                    .userId(user.getId())
                    .recipientEmail(user.getEmail())
                    .type("FORGOT_PASSWORD_OTP")
                    .title("Mã OTP đặt lại mật khẩu StyleMind")
                    .content("Mã OTP của bạn là: [PROTECTED]. Mã có hiệu lực trong " + resetOtpExpiryMinutes + " phút.")
                    .htmlContent("<p>Mã OTP của bạn là <strong>" + otp + "</strong>.</p><p>Mã có hiệu lực trong " + resetOtpExpiryMinutes + " phút.</p>")
                    .build());
        } catch (Exception ex) {
            log.warn("Failed to send forgot-password OTP email to user {}: {}", user.getId(), ex.getMessage());
            // Do NOT rethrow — the OTP is already saved; the user can retry the forgot-password request
        }
    }

    private void clearForgotPasswordState(User user) {
        user.setPasswordResetOtpHash(null);
        user.setPasswordResetOtpExpiresAt(null);
        user.setPasswordResetOtpAttempts(0);
        user.setPasswordResetRequestedAt(null);
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetTokenExpiresAt(null);
    }

    private void clearPasswordSetupState(User user) {
        user.setPasswordSetupRequired(false);
        user.setPasswordSetupTokenHash(null);
        user.setPasswordSetupTokenExpiresAt(null);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }
}
