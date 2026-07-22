package com.stylemind.auth.service;

import com.stylemind.auth.dto.AdminCreateUserRequest;
import com.stylemind.auth.dto.AdminUserResponse;
import com.stylemind.auth.dto.AdminUserSummaryResponse;
import com.stylemind.auth.dto.AuthResponse;
import com.stylemind.auth.dto.ForgotPasswordRequest;
import com.stylemind.auth.dto.ForgotPasswordVerifyResponse;
import com.stylemind.auth.dto.InternalUserEmailResponse;
import com.stylemind.auth.dto.LoginRequest;
import com.stylemind.auth.dto.PasswordSetupRequest;
import com.stylemind.auth.dto.RegisterRequest;
import com.stylemind.auth.dto.ResendRegisterOtpRequest;
import com.stylemind.auth.dto.ResetForgotPasswordRequest;
import com.stylemind.auth.dto.UserResponse;
import com.stylemind.auth.dto.VerifyForgotPasswordOtpRequest;
import com.stylemind.auth.dto.VerifyRegisterOtpRequest;
import com.stylemind.common.dto.PageResponse;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface AuthService extends UserDetailsService {

    AuthResponse.LoginResponse login(LoginRequest request);

    void startRegistration(RegisterRequest request);

    void verifyRegistrationOtp(VerifyRegisterOtpRequest request);

    void resendRegistrationOtp(ResendRegisterOtpRequest request);

    UserResponse getCurrentUser(String userId);

    InternalUserEmailResponse getUserEmail(String userId);

    void setupPassword(PasswordSetupRequest request);

    void requestForgotPasswordOtp(ForgotPasswordRequest request);

    ForgotPasswordVerifyResponse verifyForgotPasswordOtp(VerifyForgotPasswordOtpRequest request);

    void resetForgotPassword(ResetForgotPasswordRequest request);

    PageResponse<AdminUserResponse> listUsers(int page, int size, String search, String role, Boolean enabled);

    AdminUserResponse getUserById(String userId);

    AdminUserSummaryResponse getUserSummary();

    AdminUserResponse createUserByAdmin(AdminCreateUserRequest request, String requesterId);

    AdminUserResponse changeUserRole(String userId, String requesterId, String newRole);

    AdminUserResponse changeUserEnabled(String userId, String requesterId, Boolean enabled);

    void deleteUser(String userId, String requesterId);
}
