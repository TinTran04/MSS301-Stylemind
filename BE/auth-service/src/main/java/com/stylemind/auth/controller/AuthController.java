package com.stylemind.auth.controller;

import com.stylemind.auth.dto.AuthResponse;
import com.stylemind.auth.dto.ForgotPasswordRequest;
import com.stylemind.auth.dto.ForgotPasswordVerifyResponse;
import com.stylemind.auth.dto.LoginRequest;
import com.stylemind.auth.dto.PasswordSetupRequest;
import com.stylemind.auth.dto.RegisterRequest;
import com.stylemind.auth.dto.ResetForgotPasswordRequest;
import com.stylemind.auth.dto.UserResponse;
import com.stylemind.auth.dto.VerifyForgotPasswordOtpRequest;
import com.stylemind.auth.service.AuthService;
import com.stylemind.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.stylemind.common.security.UserPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse.LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse.LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse.LoginResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse.LoginResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công", response));
    }

    @PostMapping("/password/setup")
    public ResponseEntity<ApiResponse<Void>> setupPassword(@Valid @RequestBody PasswordSetupRequest request) {
        authService.setupPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Thiết lập mật khẩu thành công", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestForgotPasswordOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Nếu email tồn tại, mã OTP đã được gửi", null));
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<ApiResponse<ForgotPasswordVerifyResponse>> verifyForgotPasswordOtp(
            @Valid @RequestBody VerifyForgotPasswordOtpRequest request) {
        ForgotPasswordVerifyResponse response = authService.verifyForgotPasswordOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Xác thực OTP thành công", response));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetForgotPassword(@Valid @RequestBody ResetForgotPasswordRequest request) {
        authService.resetForgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Đặt lại mật khẩu thành công", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse user = authService.getCurrentUser(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", user));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        // JWT is stateless, client should delete token
        // Optionally add token to blacklist in Redis
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }
}
