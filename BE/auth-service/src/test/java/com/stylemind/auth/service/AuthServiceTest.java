package com.stylemind.auth.service;

import com.stylemind.auth.dto.AdminCreateUserRequest;
import com.stylemind.auth.dto.ForgotPasswordRequest;
import com.stylemind.auth.dto.LoginRequest;
import com.stylemind.auth.dto.PasswordSetupRequest;
import com.stylemind.auth.dto.RegisterRequest;
import com.stylemind.auth.dto.ResetForgotPasswordRequest;
import com.stylemind.auth.dto.VerifyForgotPasswordOtpRequest;
import com.stylemind.auth.entity.User;
import com.stylemind.auth.feign.NotificationInternalClient;
import com.stylemind.auth.repository.UserRepository;
import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock ObjectProvider<AuthenticationManager> authManagerProvider;
    @Mock AuthenticationManager authenticationManager;
    @Mock NotificationInternalClient notificationInternalClient;

    @InjectMocks AuthService authService;

    @BeforeEach
    void setUp() {
        when(authManagerProvider.getObject()).thenReturn(authenticationManager);
        lenient().when(notificationInternalClient.sendEmail(any())).thenReturn(ApiResponse.success("ok", null));
        ReflectionTestUtils.setField(authService, "frontendBaseUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(authService, "setupTokenExpiryMinutes", 1440L);
        ReflectionTestUtils.setField(authService, "resetOtpExpiryMinutes", 10L);
        ReflectionTestUtils.setField(authService, "resetTokenExpiryMinutes", 30L);
        ReflectionTestUtils.setField(authService, "resetOtpMaxAttempts", 5);
        ReflectionTestUtils.setField(authService, "resetOtpResendCooldownSeconds", 60L);
    }

    @Test
    void register_success() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getCreatedAt() == null) u.setCreatedAt(LocalDateTime.now());
            if (u.getUpdatedAt() == null) u.setUpdatedAt(LocalDateTime.now());
            return u;
        });
        when(jwtUtil.generateAccessToken(any(), any(), any())).thenReturn("token-xyz");

        var req = new RegisterRequest();
        req.setEmail("new@example.com");
        req.setPassword("pass");
        req.setName("Alice");

        var result = authService.register(req);

        assertThat(result.getToken()).isEqualTo("token-xyz");
        assertThat(result.getUser().getEmail()).isEqualTo("new@example.com");
        assertThat(result.getUser().getRole()).isEqualTo("CUSTOMER");
    }

    @Test
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        var req = new RegisterRequest();
        req.setEmail("dup@example.com");
        req.setPassword("pass");
        req.setName("Bob");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email đã được sử dụng");
    }

    @Test
    void login_success() {
        User user = activeUser("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(any(), any(), any())).thenReturn("jwt-token");

        var req = loginReq("alice@example.com", "password");
        var result = authService.login(req);

        assertThat(result.getToken()).isEqualTo("jwt-token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_disabledAccount_throws() {
        User user = activeUser("locked@example.com");
        user.setEnabled(false);
        when(userRepository.findByEmail("locked@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(loginReq("locked@example.com", "pw")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tài khoản đã bị khóa");
    }

    @Test
    void login_wrongCredentials_returnsFriendlyBusinessException() {
        doThrow(new BadCredentialsException("bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(loginReq("x@x.com", "wrong")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email hoặc mật khẩu không đúng");
    }

    @Test
    void createUserByAdmin_setsPasswordSetupAndSendsEmail() {
        when(userRepository.existsByEmail("invite@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            if (user.getCreatedAt() == null) user.setCreatedAt(LocalDateTime.now());
            if (user.getUpdatedAt() == null) user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        var request = AdminCreateUserRequest.builder()
                .email("invite@example.com")
                .fullName("Invited User")
                .role("CUSTOMER")
                .build();

        var response = authService.createUserByAdmin(request, "admin-1");

        assertThat(response.getEmail()).isEqualTo("invite@example.com");
        assertThat(response.getPasswordSetupRequired()).isTrue();
        verify(notificationInternalClient).sendEmail(argThat(payload ->
                "invite@example.com".equals(payload.getRecipientEmail())
                        && "USER_INVITE".equals(payload.getType())));
    }

    @Test
    void setupPassword_successClearsSetupState() {
        User user = activeUser("setup@example.com");
        user.setPasswordSetupRequired(true);
        user.setPasswordSetupTokenHash("hashed-setup-token");
        user.setPasswordSetupTokenExpiresAt(LocalDateTime.now().plusHours(1));
        when(userRepository.findByEmail("setup@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("setup-token", "hashed-setup-token")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-password");

        authService.setupPassword(PasswordSetupRequest.builder()
                .email("setup@example.com")
                .token("setup-token")
                .newPassword("new-password")
                .build());

        assertThat(user.getPasswordSetupRequired()).isFalse();
        assertThat(user.getPasswordSetupTokenHash()).isNull();
        assertThat(user.getPasswordHash()).isEqualTo("encoded-password");
    }

    @Test
    void requestForgotPasswordOtp_existingUserStoresOtpAndSendsMail() {
        User user = activeUser("reset@example.com");
        when(userRepository.findByEmail("reset@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(any())).thenReturn("encoded-otp");

        authService.requestForgotPasswordOtp(ForgotPasswordRequest.builder()
                .email("reset@example.com")
                .build());

        assertThat(user.getPasswordResetOtpHash()).isEqualTo("encoded-otp");
        verify(notificationInternalClient).sendEmail(argThat(payload ->
                "reset@example.com".equals(payload.getRecipientEmail())
                        && "FORGOT_PASSWORD_OTP".equals(payload.getType())));
    }

    @Test
    void verifyForgotPasswordOtp_successReturnsResetToken() {
        User user = activeUser("verify@example.com");
        user.setPasswordResetOtpHash("hashed-otp");
        user.setPasswordResetOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        user.setPasswordResetOtpAttempts(0);
        when(userRepository.findByEmail("verify@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "hashed-otp")).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("encoded-reset-token");

        var response = authService.verifyForgotPasswordOtp(VerifyForgotPasswordOtpRequest.builder()
                .email("verify@example.com")
                .otp("123456")
                .build());

        assertThat(response.getResetToken()).isNotBlank();
        assertThat(user.getPasswordResetTokenHash()).isEqualTo("encoded-reset-token");
        assertThat(user.getPasswordResetOtpHash()).isNull();
    }

    @Test
    void resetForgotPassword_successUpdatesPassword() {
        User user = activeUser("final@example.com");
        user.setPasswordResetTokenHash("hashed-token");
        user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
        when(userRepository.findByEmail("final@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("reset-token", "hashed-token")).thenReturn(true);
        when(passwordEncoder.encode("brand-new-password")).thenReturn("encoded-brand-new-password");

        authService.resetForgotPassword(ResetForgotPasswordRequest.builder()
                .email("final@example.com")
                .resetToken("reset-token")
                .newPassword("brand-new-password")
                .build());

        assertThat(user.getPasswordHash()).isEqualTo("encoded-brand-new-password");
        assertThat(user.getPasswordResetTokenHash()).isNull();
    }

    @Test
    void changeRole_selfChange_throws() {
        assertThatThrownBy(() -> authService.changeUserRole("user-1", "user-1", "ADMIN"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể tự thay đổi role");
    }

    @Test
    void changeEnabled_selfBan_throws() {
        assertThatThrownBy(() -> authService.changeUserEnabled("user-1", "user-1", false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể tự ban");
    }

    @Test
    void changeRole_success() {
        User user = activeUser("target@example.com");
        user.setId("target-id");
        when(userRepository.findById("target-id")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = authService.changeUserRole("target-id", "admin-id", "ADMIN");

        assertThat(result.getRole()).isEqualTo("ADMIN");
    }

    // ─── Additional missing test cases ───────────────────────────────────────

    @Test
    void forgotPassword_unknownEmail_returnsGenericallySilent() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
        // Must NOT throw — silent generic response
        authService.requestForgotPasswordOtp(ForgotPasswordRequest.builder().email("ghost@example.com").build());
    }

    @Test
    void forgotPassword_cooldownEnforced_doesNotResend() {
        User user = activeUser("cool@example.com");
        // Set last request to 30s ago — within 60s cooldown
        user.setPasswordResetRequestedAt(LocalDateTime.now().minusSeconds(30));
        when(userRepository.findByEmail("cool@example.com")).thenReturn(Optional.of(user));

        authService.requestForgotPasswordOtp(ForgotPasswordRequest.builder().email("cool@example.com").build());

        // No new OTP should be saved or email sent
        org.mockito.Mockito.verifyNoInteractions(notificationInternalClient);
    }

    @Test
    void verifyOtp_expiredOtp_throws() {
        User user = activeUser("expired@example.com");
        user.setPasswordResetOtpHash("hashed-otp");
        // OTP expired 1 minute ago
        user.setPasswordResetOtpExpiresAt(LocalDateTime.now().minusMinutes(1));
        user.setPasswordResetOtpAttempts(0);
        when(userRepository.findByEmail("expired@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.verifyForgotPasswordOtp(
                VerifyForgotPasswordOtpRequest.builder().email("expired@example.com").otp("123456").build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OTP không hợp lệ hoặc đã hết hạn");
    }

    @Test
    void verifyOtp_wrongOtp_incrementsAttempts() {
        User user = activeUser("wrong@example.com");
        user.setPasswordResetOtpHash("hashed-otp");
        user.setPasswordResetOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        user.setPasswordResetOtpAttempts(2);
        when(userRepository.findByEmail("wrong@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("999999", "hashed-otp")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> authService.verifyForgotPasswordOtp(
                VerifyForgotPasswordOtpRequest.builder().email("wrong@example.com").otp("999999").build()))
                .isInstanceOf(BusinessException.class);

        assertThat(user.getPasswordResetOtpAttempts()).isEqualTo(3);
    }

    @Test
    void verifyOtp_maxAttemptsReached_blocked() {
        User user = activeUser("blocked@example.com");
        user.setPasswordResetOtpHash("hashed-otp");
        user.setPasswordResetOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        user.setPasswordResetOtpAttempts(5); // already at max
        when(userRepository.findByEmail("blocked@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.verifyForgotPasswordOtp(
                VerifyForgotPasswordOtpRequest.builder().email("blocked@example.com").otp("123456").build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OTP đã bị khóa");
    }

    @Test
    void resetPassword_expiredToken_throws() {
        User user = activeUser("expired-token@example.com");
        user.setPasswordResetTokenHash("hashed-token");
        user.setPasswordResetTokenExpiresAt(LocalDateTime.now().minusMinutes(1)); // expired
        when(userRepository.findByEmail("expired-token@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.resetForgotPassword(ResetForgotPasswordRequest.builder()
                .email("expired-token@example.com")
                .resetToken("reset-token")
                .newPassword("newpassword123")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không hợp lệ hoặc đã hết hạn");
    }

    @Test
    void resetPassword_wrongToken_throws() {
        User user = activeUser("wrong-token@example.com");
        user.setPasswordResetTokenHash("hashed-token");
        user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
        when(userRepository.findByEmail("wrong-token@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-token", "hashed-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.resetForgotPassword(ResetForgotPasswordRequest.builder()
                .email("wrong-token@example.com")
                .resetToken("wrong-token")
                .newPassword("newpassword123")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không hợp lệ hoặc đã hết hạn");
    }

    @Test
    void setupPassword_expiredToken_throws() {
        User user = activeUser("expired-setup@example.com");
        user.setPasswordSetupRequired(true);
        user.setPasswordSetupTokenHash("hashed-setup");
        user.setPasswordSetupTokenExpiresAt(LocalDateTime.now().minusHours(1)); // expired
        when(userRepository.findByEmail("expired-setup@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.setupPassword(PasswordSetupRequest.builder()
                .email("expired-setup@example.com")
                .token("any-token")
                .newPassword("newpass123")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không hợp lệ hoặc đã hết hạn");
    }

    @Test
    void setupPassword_wrongToken_throws() {
        User user = activeUser("wrong-setup@example.com");
        user.setPasswordSetupRequired(true);
        user.setPasswordSetupTokenHash("hashed-setup");
        user.setPasswordSetupTokenExpiresAt(LocalDateTime.now().plusHours(1));
        when(userRepository.findByEmail("wrong-setup@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-token", "hashed-setup")).thenReturn(false);

        assertThatThrownBy(() -> authService.setupPassword(PasswordSetupRequest.builder()
                .email("wrong-setup@example.com")
                .token("wrong-token")
                .newPassword("newpass123")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không hợp lệ hoặc đã hết hạn");
    }

    @Test
    void getCurrentUser_returnsEnabledField() {
        User user = activeUser("me@example.com");
        user.setEnabled(true);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        var result = authService.getCurrentUser("user-1");

        assertThat(result.getEnabled()).isTrue();
        assertThat(result.getEmail()).isEqualTo("me@example.com");
    }

    private User activeUser(String email) {
        User u = new User();
        u.setId("user-1");
        u.setEmail(email);
        u.setPasswordHash("hashed");
        u.setRole("CUSTOMER");
        u.setEnabled(true);
        u.setPasswordSetupRequired(false);
        u.setProvider("LOCAL");
        u.setFullName("Test User");
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        return u;
    }

    private LoginRequest loginReq(String email, String password) {
        var r = new LoginRequest();
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }
}
