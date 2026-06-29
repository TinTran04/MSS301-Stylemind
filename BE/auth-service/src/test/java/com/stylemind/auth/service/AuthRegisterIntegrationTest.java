package com.stylemind.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stylemind.auth.dto.RegisterRequest;
import com.stylemind.auth.dto.RegisterResponse;
import com.stylemind.auth.entity.Account;
import com.stylemind.auth.entity.AccountRole;
import com.stylemind.auth.entity.AccountStatus;
import com.stylemind.auth.entity.OutboxEvent;
import com.stylemind.auth.entity.OutboxEventStatus;
import com.stylemind.auth.repository.AccountRepository;
import com.stylemind.auth.repository.OutboxEventRepository;
import com.stylemind.common.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthRegisterIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void registerCreatesAccountAndUserRegisteredOutboxEvent() throws Exception {
        RegisterResponse response = authService.register(registerRequest(" Customer@Example.COM ", "Nguyen Van A"));

        assertThat(response.getEmail()).isEqualTo("customer@example.com");
        assertThat(response.getRole()).isEqualTo(AccountRole.CUSTOMER.name());
        assertThat(response.getStatus()).isEqualTo(AccountStatus.PENDING.name());
        assertThat(response.isEmailVerified()).isFalse();

        Account account = accountRepository.findByEmail("customer@example.com").orElseThrow();
        assertThat(account.getId().toString()).isEqualTo(response.getId());
        assertThat(account.getRole()).isEqualTo(AccountRole.CUSTOMER);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.PENDING);
        assertThat(account.isEmailVerified()).isFalse();
        assertThat(account.getPasswordHash()).isNotEqualTo("StrongPassword123!");
        assertThat(passwordEncoder.matches("StrongPassword123!", account.getPasswordHash())).isTrue();

        List<OutboxEvent> events = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);
        assertThat(events).hasSize(1);

        OutboxEvent event = events.get(0);
        assertThat(event.getAggregateId()).isEqualTo(account.getId());
        assertThat(event.getEventType()).isEqualTo("USER_REGISTERED");
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);

        JsonNode payload = objectMapper.readTree(event.getPayload());
        assertThat(payload.get("eventId").asText()).isEqualTo(event.getId().toString());
        assertThat(payload.get("eventType").asText()).isEqualTo("USER_REGISTERED");
        assertThat(payload.get("occurredAt").asText()).isNotBlank();
        assertThat(payload.at("/data/userId").asText()).isEqualTo(account.getId().toString());
        assertThat(payload.at("/data/fullName").asText()).isEqualTo("Nguyen Van A");
        assertThat(event.getPayload()).doesNotContain("password", "password_hash", "refresh", "phone", "address");
    }

    @Test
    void registerReturnsConflictWhenEmailAlreadyExists() {
        authService.register(registerRequest("customer@example.com", "First Customer"));

        assertThatThrownBy(() -> authService.register(registerRequest("customer@example.com", "Second Customer")))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("EMAIL_ALREADY_EXISTS");
                    assertThat(ex.getHttpStatus()).isEqualTo(409);
                });
    }

    @Test
    void registerTreatsEmailCaseInsensitiveForDuplicates() {
        authService.register(registerRequest("Customer@Example.com", "First Customer"));

        assertThatThrownBy(() -> authService.register(registerRequest("customer@example.COM", "Second Customer")))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("EMAIL_ALREADY_EXISTS");
                    assertThat(ex.getHttpStatus()).isEqualTo(409);
                });
    }

    @Test
    void registerRejectsPasswordShorterThanConfiguredMinimum() {
        assertThatThrownBy(() -> authService.register(registerRequest("customer@example.com", "Nguyen Van A", "short")))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("INVALID_PASSWORD");
                    assertThat(ex.getHttpStatus()).isEqualTo(400);
                });

        assertThat(accountRepository.existsByEmail("customer@example.com")).isFalse();
        assertThat(outboxEventRepository.count()).isZero();
    }

    @Test
    void registerResponseDoesNotExposePasswordHash() throws Exception {
        RegisterResponse response = authService.register(registerRequest("customer@example.com", "Nguyen Van A"));

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).doesNotContain("password", "passwordHash", "password_hash");
    }

    private RegisterRequest registerRequest(String email, String fullName) {
        return registerRequest(email, fullName, "StrongPassword123!");
    }

    private RegisterRequest registerRequest(String email, String fullName, String password) {
        return RegisterRequest.builder()
                .email(email)
                .password(password)
                .fullName(fullName)
                .build();
    }
}
