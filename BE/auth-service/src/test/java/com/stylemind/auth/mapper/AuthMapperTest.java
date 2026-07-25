package com.stylemind.auth.mapper;

import com.stylemind.auth.dto.UserResponse;
import com.stylemind.auth.entity.AccountStatus;
import com.stylemind.auth.entity.User;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class AuthMapperTest {

    private final AuthMapper authMapper = new AuthMapper();

    @Test
    void toUserResponse_mapsOnlyPublicIdentityFields() {
        User user = activeUser();

        UserResponse response = authMapper.toUserResponse(user);

        assertThat(response.getId()).isEqualTo("user-1");
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(Arrays.stream(UserResponse.class.getDeclaredFields()).map(Field::getName))
                .doesNotContain(
                        "passwordHash",
                        "passwordResetOtpHash",
                        "passwordResetTokenHash",
                        "passwordSetupTokenHash");
    }

    @Test
    void toPrincipal_keepsCredentialsInsideSecurityBoundary() {
        var principal = authMapper.toPrincipal(activeUser());

        assertThat(principal.getUserId()).isEqualTo("user-1");
        assertThat(principal.getPassword()).isEqualTo("bcrypt-hash");
        assertThat(principal.isEnabled()).isTrue();
    }

    private User activeUser() {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .id("user-1")
                .email("user@example.com")
                .passwordHash("bcrypt-hash")
                .provider("LOCAL")
                .role("CUSTOMER")
                .accountStatus(AccountStatus.ACTIVE)
                .passwordSetupRequired(false)
                .build();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }
}
