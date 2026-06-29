package com.stylemind.auth.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stylemind.auth.entity.Account;
import com.stylemind.auth.entity.AccountRole;
import com.stylemind.auth.entity.AccountStatus;
import com.stylemind.auth.entity.RefreshToken;
import com.stylemind.auth.repository.AccountRepository;
import com.stylemind.auth.repository.RefreshTokenRepository;
import com.stylemind.common.security.JwtUtil;
import com.stylemind.common.security.UserPrincipal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void adminCanUpdateAccountStatusAndRevokeActiveSessions() throws Exception {
        Account target = saveAccount(AccountRole.CUSTOMER, AccountStatus.ACTIVE);
        saveRefreshToken(target);

        mockMvc.perform(patch("/api/admin/users/{userId}/status", target.getId())
                        .header("Authorization", bearerToken(saveAccount(AccountRole.ADMIN, AccountStatus.ACTIVE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountStatus": "LOCKED",
                                  "reason": "Manual admin action"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(target.getId().toString())))
                .andExpect(jsonPath("$.data.accountStatus", is("LOCKED")))
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        Account updated = accountRepository.findById(target.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getStatus()).isEqualTo(AccountStatus.LOCKED);
        org.assertj.core.api.Assertions.assertThat(refreshTokenRepository.findByAccountIdAndRevokedAtIsNull(target.getId()))
                .isEmpty();
    }

    @Test
    void adminCanUpdateAccountRoleAndRevokeActiveSessions() throws Exception {
        Account target = saveAccount(AccountRole.CUSTOMER, AccountStatus.ACTIVE);
        saveRefreshToken(target);

        mockMvc.perform(patch("/api/admin/users/{userId}/role", target.getId())
                        .header("Authorization", bearerToken(saveAccount(AccountRole.ADMIN, AccountStatus.ACTIVE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "STAFF",
                                  "reason": "Promoted to support"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(target.getId().toString())))
                .andExpect(jsonPath("$.data.role", is("STAFF")))
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        Account updated = accountRepository.findById(target.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getRole()).isEqualTo(AccountRole.STAFF);
        org.assertj.core.api.Assertions.assertThat(updated.getTokenVersion()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(refreshTokenRepository.findByAccountIdAndRevokedAtIsNull(target.getId()))
                .isEmpty();
    }

    private Account saveAccount(AccountRole role, AccountStatus status) {
        return accountRepository.saveAndFlush(Account.builder()
                .id(UUID.randomUUID())
                .email(UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("StrongPassword123!"))
                .role(role)
                .status(status)
                .emailVerified(status == AccountStatus.ACTIVE)
                .tokenVersion(0)
                .build());
    }

    private void saveRefreshToken(Account account) {
        refreshTokenRepository.saveAndFlush(RefreshToken.builder()
                .id(UUID.randomUUID())
                .account(account)
                .tokenHash(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build());
    }

    private String bearerToken(Account account) {
        UserDetails userDetails = new UserPrincipal(
                account.getId().toString(),
                account.getEmail(),
                "",
                account.getRole().name(),
                "JWT",
                true);
        return "Bearer " + jwtUtil.generateAccessToken(userDetails, account.getId().toString(), account.getRole().name());
    }
}
