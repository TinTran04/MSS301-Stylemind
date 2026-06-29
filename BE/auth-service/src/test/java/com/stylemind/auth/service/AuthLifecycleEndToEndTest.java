package com.stylemind.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stylemind.auth.dto.AuthResponse;
import com.stylemind.auth.dto.LoginRequest;
import com.stylemind.auth.dto.RegisterRequest;
import com.stylemind.auth.dto.RegisterResponse;
import com.stylemind.auth.entity.Account;
import com.stylemind.auth.entity.AccountStatus;
import com.stylemind.auth.entity.OutboxEvent;
import com.stylemind.auth.entity.OutboxEventStatus;
import com.stylemind.auth.entity.RefreshToken;
import com.stylemind.auth.repository.AccountRepository;
import com.stylemind.auth.repository.EmailVerificationTokenRepository;
import com.stylemind.auth.repository.OutboxEventRepository;
import com.stylemind.auth.repository.PasswordResetTokenRepository;
import com.stylemind.auth.repository.RefreshTokenRepository;
import com.stylemind.common.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "auth.outbox.publisher.enabled=true",
        "auth.outbox.publisher.initial-delay-ms=600000",
        "auth.email-verification.required=false"
})
@ActiveProfiles("test")
class AuthLifecycleEndToEndTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private OutboxPublisherService outboxPublisherService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private EmailSender emailSender;

    @BeforeEach
    void cleanDatabase() {
        passwordResetTokenRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        outboxEventRepository.deleteAll();
        accountRepository.deleteAll();
        reset(rabbitTemplate, emailSender);
        when(rabbitTemplate.invoke(any())).thenReturn(null);
    }

    @Test
    void registerPublishLoginRefreshLogoutAndLogoutAllJourney() throws Exception {
        RegisterResponse registered = authService.register(registerRequest());

        Account account = accountRepository.findByEmail("customer@example.com").orElseThrow();
        assertThat(account.getId().toString())
                .as("registered account id should match database account")
                .isEqualTo(registered.getId());

        OutboxEvent event = onlyOutboxEvent();
        assertThat(event.getStatus()).as("USER_REGISTERED should start pending").isEqualTo(OutboxEventStatus.PENDING);
        JsonNode payload = objectMapper.readTree(event.getPayload());
        assertThat(payload.at("/data/userId").asText()).isEqualTo(account.getId().toString());
        assertThat(payload.at("/data/fullName").asText()).isEqualTo("Nguyen Van A");
        assertThat(event.getPayload()).doesNotContain("password", "phone", "address", "refreshToken");

        int published = outboxPublisherService.publishPendingBatch();
        OutboxEvent publishedEvent = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(published).as("outbox publisher should publish the registration event").isEqualTo(1);
        assertThat(publishedEvent.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(publishedEvent.getPublishedAt()).isNotNull();

        account.setStatus(AccountStatus.ACTIVE);
        account.setEmailVerified(true);
        accountRepository.save(account);

        AuthResponse.LoginResponse login = authService.login(loginRequest());
        assertThat(login.getAccessToken()).isNotBlank();
        assertThat(login.getRefreshToken()).isNotBlank();
        assertThat(activeRefreshTokens(account)).hasSize(1);
        assertThat(activeRefreshTokens(account).get(0).getTokenHash()).isNotEqualTo(login.getRefreshToken());

        AuthResponse.LoginResponse refreshed = authService.refresh(login.getRefreshToken());
        assertThat(refreshed.getAccessToken()).isNotEqualTo(login.getAccessToken());
        assertThat(refreshed.getRefreshToken()).isNotEqualTo(login.getRefreshToken());

        authService.logout(refreshed.getRefreshToken());
        assertThat(activeRefreshTokens(account)).isEmpty();
        assertInvalidRefreshToken(() -> authService.refresh(login.getRefreshToken()));
        assertInvalidRefreshToken(() -> authService.refresh(refreshed.getRefreshToken()));

        AuthResponse.LoginResponse firstRelogin = authService.login(loginRequest());
        AuthResponse.LoginResponse secondRelogin = authService.login(loginRequest());
        assertThat(activeRefreshTokens(account)).hasSize(2);

        authService.logoutAll(account.getId().toString());

        assertThat(activeRefreshTokens(account)).isEmpty();
        assertInvalidRefreshToken(() -> authService.refresh(firstRelogin.getRefreshToken()));
        assertInvalidRefreshToken(() -> authService.refresh(secondRelogin.getRefreshToken()));
    }

    private OutboxEvent onlyOutboxEvent() {
        List<OutboxEvent> events = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);
        assertThat(events).as("one pending USER_REGISTERED outbox event should exist").hasSize(1);
        return events.get(0);
    }

    private List<RefreshToken> activeRefreshTokens(Account account) {
        return refreshTokenRepository.findByAccountIdAndRevokedAtIsNull(account.getId());
    }

    private void assertInvalidRefreshToken(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("INVALID_REFRESH_TOKEN");
                    assertThat(ex.getHttpStatus()).isEqualTo(401);
                });
    }

    private RegisterRequest registerRequest() {
        return RegisterRequest.builder()
                .email(" Customer@Example.COM ")
                .password("StrongPassword123!")
                .fullName("Nguyen Van A")
                .build();
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("customer@example.com");
        request.setPassword("StrongPassword123!");
        return request;
    }
}
