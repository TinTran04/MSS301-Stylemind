package com.stylemind.common.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    void passwordEncoder_usesBcryptCostTwelve() {
        SecurityConfig securityConfig = new SecurityConfig(null, null);

        String passwordHash = securityConfig.passwordEncoder().encode("test-password");

        assertThat(passwordHash).startsWith("$2a$12$");
    }
}
