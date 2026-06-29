package com.stylemind.auth.controller;

import com.stylemind.auth.dto.AuthResponse;
import com.stylemind.auth.dto.ChangePasswordRequest;
import com.stylemind.auth.dto.ForgotPasswordRequest;
import com.stylemind.auth.dto.LoginRequest;
import com.stylemind.auth.dto.RefreshTokenRequest;
import com.stylemind.auth.dto.RegisterRequest;
import com.stylemind.auth.dto.RegisterResponse;
import com.stylemind.auth.dto.ResetPasswordRequest;
import com.stylemind.auth.dto.UserResponse;
import com.stylemind.auth.dto.VerifyEmailRequest;
import com.stylemind.auth.service.AuthService;
import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${auth.refresh-token.cookie-name:refresh_token}")
    private String refreshTokenCookieName;

    @Value("${auth.refresh-token.cookie-secure:false}")
    private boolean refreshTokenCookieSecure;

    @Value("${auth.refresh-token.cookie-domain:}")
    private String refreshTokenCookieDomain;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse.LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse.LoginResponse response = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(response).toString())
                .body(ApiResponse.success("Login successfully", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse.LoginResponse>> refresh(
            @CookieValue(name = "${auth.refresh-token.cookie-name:refresh_token}", required = false) String cookieToken,
            @RequestBody(required = false) RefreshTokenRequest request) {
        AuthResponse.LoginResponse response = authService.refresh(resolveRefreshToken(cookieToken, request));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(response).toString())
                .body(ApiResponse.success("Refresh token successfully", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Register successfully", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse user = authService.getCurrentUser(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Get current user successfully", user));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> logout(
            @CookieValue(name = "${auth.refresh-token.cookie-name:refresh_token}", required = false) String cookieToken,
            @RequestBody(required = false) RefreshTokenRequest request) {
        authService.logout(resolveRefreshToken(cookieToken, request));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .body(ApiResponse.success("Logout successfully", Map.of("revoked", true)));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Map<String, Long>>> logoutAll(@AuthenticationPrincipal UserPrincipal principal) {
        long revokedSessions = authService.logoutAll(principal.getUserId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .body(ApiResponse.success("Logout all successfully", Map.of("revokedSessions", revokedSessions)));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getUserId(), request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .body(ApiResponse.success("Change password successfully", Map.of("changed", true)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.accepted()
                .body(ApiResponse.success(
                        "If the account exists, password reset instructions will be sent",
                        Map.of("accepted", true)));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Reset password successfully", Map.of("reset", true)));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Verify email successfully", Map.of(
                "verified", true,
                "accountStatus", authService.verifyEmail(request).name())));
    }

    private String resolveRefreshToken(String cookieToken, RefreshTokenRequest request) {
        if (cookieToken != null && !cookieToken.isBlank()) {
            return cookieToken;
        }
        return request == null ? null : request.getRefreshToken();
    }

    private ResponseCookie buildRefreshCookie(AuthResponse.LoginResponse response) {
        ResponseCookie.ResponseCookieBuilder builder = baseRefreshCookie(response.getRefreshToken())
                .maxAge(response.getRefreshExpiresInSeconds());
        return builder.build();
    }

    private ResponseCookie clearRefreshCookie() {
        return baseRefreshCookie("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseRefreshCookie(String value) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshTokenCookieName, value)
                .httpOnly(true)
                .secure(refreshTokenCookieSecure)
                .sameSite("Strict")
                .path("/api/auth");
        if (refreshTokenCookieDomain != null && !refreshTokenCookieDomain.isBlank()) {
            builder.domain(refreshTokenCookieDomain);
        }
        return builder;
    }
}
