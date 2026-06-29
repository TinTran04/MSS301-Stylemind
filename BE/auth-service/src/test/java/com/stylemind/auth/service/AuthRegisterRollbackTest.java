package com.stylemind.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.stylemind.auth.dto.RegisterRequest;
import com.stylemind.auth.entity.OutboxEvent;
import com.stylemind.auth.repository.AccountRepository;
import com.stylemind.auth.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthRegisterRollbackTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountRepository accountRepository;

    @MockBean
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void cleanDatabase() {
        accountRepository.deleteAll();
    }

    @Test
    void registerRollsBackAccountWhenOutboxCreationFails() {
        when(outboxEventRepository.save(any(OutboxEvent.class)))
                .thenThrow(new IllegalStateException("outbox unavailable"));

        RegisterRequest request = RegisterRequest.builder()
                .email("rollback@example.com")
                .password("StrongPassword123!")
                .fullName("Rollback User")
                .build();

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("outbox unavailable");

        assertThat(accountRepository.existsByEmail("rollback@example.com")).isFalse();
    }
}
