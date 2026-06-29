package com.stylemind.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stylemind.auth.dto.RegisterRequest;
import com.stylemind.auth.entity.Account;
import com.stylemind.auth.entity.OutboxEvent;
import com.stylemind.auth.repository.AccountRepository;
import com.stylemind.auth.repository.EmailVerificationTokenRepository;
import com.stylemind.auth.repository.OutboxEventRepository;
import com.stylemind.auth.repository.PasswordResetTokenRepository;
import com.stylemind.auth.repository.RefreshTokenRepository;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthRegisterRaceConditionTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ObjectProvider<AuthenticationManager> authenticationManagerProvider;

    @Mock
    private EmailSender emailSender;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                accountRepository,
                outboxEventRepository,
                refreshTokenRepository,
                emailVerificationTokenRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                jwtUtil,
                new ObjectMapper(),
                emailSender
        );
        ReflectionTestUtils.setField(authService, "passwordMinLength", 8);
    }

    @Test
    void registerConvertsUniqueConstraintRaceToConflictWithoutCreatingOutbox() {
        when(accountRepository.existsByEmail("customer@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPassword123!")).thenReturn("$2a$12$hash");
        when(accountRepository.saveAndFlush(any(Account.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate email"));

        RegisterRequest request = RegisterRequest.builder()
                .email("customer@example.com")
                .password("StrongPassword123!")
                .fullName("Nguyen Van A")
                .build();

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("EMAIL_ALREADY_EXISTS");
                    assertThat(ex.getHttpStatus()).isEqualTo(409);
                });

        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }
}
