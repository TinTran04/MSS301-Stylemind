package com.stylemind.auth.service;

import com.stylemind.auth.dto.LoginRequest;
import com.stylemind.auth.dto.RegisterRequest;
import com.stylemind.auth.dto.AuthResponse;
import com.stylemind.auth.dto.UserResponse;
import com.stylemind.auth.entity.User;
import com.stylemind.auth.repository.UserRepository;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.security.JwtUtil;
import com.stylemind.common.security.UserPrincipal;
import com.stylemind.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ObjectProvider<AuthenticationManager> authenticationManagerProvider;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(this::buildUserPrincipal)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    public AuthResponse.LoginResponse login(LoginRequest request) {
        // Authenticate user
        authenticationManagerProvider.getObject().authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("AUTH_INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng", 401));

        if (!user.getEnabled()) {
            throw new BusinessException("AUTH_ACCOUNT_DISABLED", "Tài khoản đã bị khóa", 403);
        }

        String token = jwtUtil.generateAccessToken(
                buildUserPrincipal(user),
                user.getId(),
                user.getRole()
        );

        return AuthResponse.LoginResponse.builder()
                .token(token)
                .user(buildUserResponse(user))
                .build();
    }

    public AuthResponse.LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "Email đã được sử dụng", 400);
        }

        User user = User.builder()
                .id(StringUtil.generateUniqueId())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getName())
                .provider("LOCAL")
                .role("CUSTOMER")
                .enabled(true)
                .build();

        user = userRepository.save(user);

        String token = jwtUtil.generateAccessToken(
                buildUserPrincipal(user),
                user.getId(),
                user.getRole()
        );

        return AuthResponse.LoginResponse.builder()
                .token(token)
                .user(buildUserResponse(user))
                .build();
    }

    public UserResponse getCurrentUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng", 404));
        return buildUserResponse(user);
    }

    private UserPrincipal buildUserPrincipal(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.getProvider(),
                user.getEnabled()
        );
    }

    private UserResponse buildUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .provider(user.getProvider())
                .createdAt(user.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .build();
    }
}