package com.stylemind.auth.mapper;

import com.stylemind.auth.dto.AdminUserResponse;
import com.stylemind.auth.dto.UserResponse;
import com.stylemind.auth.entity.AccountStatus;
import com.stylemind.auth.entity.User;
import com.stylemind.common.security.UserPrincipal;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
public class AuthMapper {

    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .provider(user.getProvider())
                .accountStatus(user.getAccountStatus())
                .enabled(user.getAccountStatus() == AccountStatus.ACTIVE)
                .createdAt(user.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant())
                .build();
    }

    public AdminUserResponse toAdminUserResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .provider(user.getProvider())
                .accountStatus(user.getAccountStatus())
                .enabled(user.getAccountStatus() == AccountStatus.ACTIVE)
                .passwordSetupRequired(user.getPasswordSetupRequired())
                .createdAt(user.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant())
                .updatedAt(user.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant())
                .build();
    }

    public UserPrincipal toPrincipal(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.getProvider(),
                user.getAccountStatus() == AccountStatus.ACTIVE
        );
    }
}
