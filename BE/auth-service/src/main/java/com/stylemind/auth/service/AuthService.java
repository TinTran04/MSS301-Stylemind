package com.stylemind.auth.service;

import com.stylemind.auth.dto.*;
import com.stylemind.auth.entity.User;
import com.stylemind.auth.repository.UserRepository;
import com.stylemind.common.dto.PageResponse;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.security.JwtUtil;
import com.stylemind.common.security.UserPrincipal;
import com.stylemind.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

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

    // ─── Admin operations ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> listUsers(int page, int size, String search) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminUserResponse> result = userRepository
                .findAllWithSearch(search, pageable)
                .map(this::buildAdminUserResponse);
        return PageResponse.of(result);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng", 404));
        return buildAdminUserResponse(user);
    }

    public AdminUserResponse changeUserRole(String userId, String requesterId, String newRole) {
        if (userId.equals(requesterId)) {
            throw new BusinessException("ADMIN_SELF_ROLE_CHANGE", "Không thể tự thay đổi role của mình", 400);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng", 404));
        user.setRole(newRole);
        return buildAdminUserResponse(userRepository.save(user));
    }

    public AdminUserResponse changeUserEnabled(String userId, String requesterId, Boolean enabled) {
        if (userId.equals(requesterId)) {
            throw new BusinessException("ADMIN_SELF_BAN", "Không thể tự ban chính mình", 400);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng", 404));
        user.setEnabled(enabled);
        return buildAdminUserResponse(userRepository.save(user));
    }

    private AdminUserResponse buildAdminUserResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .provider(user.getProvider())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .updatedAt(user.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .build();
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