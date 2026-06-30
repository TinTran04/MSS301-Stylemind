package com.stylemind.notification.service;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.notification.dto.InternalEmailRequest;
import com.stylemind.notification.dto.NotificationRequest;
import com.stylemind.notification.entity.NotificationLog;
import com.stylemind.notification.repository.NotificationLogRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationLogRepository notificationLogRepository;
    @Mock JavaMailSender mailSender;

    @InjectMocks NotificationService notificationService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "mailEnabled", true);
        ReflectionTestUtils.setField(notificationService, "mailFrom", "no-reply@stylemind.ai");
        ReflectionTestUtils.setField(notificationService, "logFallback", true);
        mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        lenient().when(notificationLogRepository.save(any())).thenAnswer(inv -> {
            NotificationLog log = inv.getArgument(0);
            if (log.getCreatedAt() == null) {
                log.setCreatedAt(LocalDateTime.now());
            }
            if (log.getUpdatedAt() == null) {
                log.setUpdatedAt(LocalDateTime.now());
            }
            return log;
        });
    }

    @Test
    void createNotification_withoutSendEmail_keepsPendingAndNormalizesFields() {
        var request = NotificationRequest.builder()
                .userId("user-1")
                .recipientEmail("user@example.com")
                .type("system")
                .title("Hello")
                .content("World")
                .build();

        var response = notificationService.createNotification(request);

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getType()).isEqualTo("SYSTEM");
        assertThat(response.getChannel()).isEqualTo("EMAIL");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void createNotification_sendEmailWithoutRecipient_throwsBeforePersisting() {
        var request = NotificationRequest.builder()
                .type("SYSTEM")
                .content("World")
                .sendEmail(true)
                .build();

        assertThatThrownBy(() -> notificationService.createNotification(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email");

        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    void createNotification_invalidStatus_throwsValidationError() {
        var request = NotificationRequest.builder()
                .type("SYSTEM")
                .status("BOUNCED")
                .content("World")
                .build();

        assertThatThrownBy(() -> notificationService.createNotification(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Trang thai");
    }

    @Test
    void getUserNotifications_filtersToRequesterAndHidesErrorMessage() {
        var pageable = PageRequest.of(0, 20);
        var log = notification("user-1", "FAILED");
        log.setErrorMessage("smtp-password-secret");
        when(notificationLogRepository.findByUserId("user-1", pageable))
                .thenReturn(new PageImpl<>(List.of(log), pageable, 1));

        var response = notificationService.getUserNotifications("user-1", pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getUserId()).isEqualTo("user-1");
        assertThat(response.getContent().get(0).getErrorMessage()).isNull();
        verify(notificationLogRepository).findByUserId("user-1", pageable);
    }

    @Test
    void getAdminNotifications_normalizesFiltersAndIncludesErrorMessage() {
        var pageable = PageRequest.of(0, 20);
        var log = notification("user-1", "FAILED");
        log.setErrorMessage("Mail delivery failed");
        when(notificationLogRepository.search(eq("user-1"), eq("FAILED"), eq("USER_INVITE"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(log), pageable, 1));

        var response = notificationService.getAdminNotifications("user-1", "failed", "user_invite", pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getErrorMessage()).isEqualTo("Mail delivery failed");
    }

    @Test
    void sendInternalEmail_marksSentWhenMailSucceeds() {
        var response = notificationService.sendInternalEmail(InternalEmailRequest.builder()
                .userId("user-1")
                .recipientEmail("user@example.com")
                .type("USER_INVITE")
                .title("Invite")
                .content("Please join")
                .htmlContent("<p>Please join</p>")
                .build());

        assertThat(response.getStatus()).isEqualTo("SENT");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendInternalEmail_replacesProtectedOtpForDeliveryWithoutPersistingIt() throws Exception {
        var response = notificationService.sendInternalEmail(InternalEmailRequest.builder()
                .userId("user-1")
                .recipientEmail("user@example.com")
                .type("FORGOT_PASSWORD_OTP")
                .title("OTP")
                .content("Your OTP is [PROTECTED]")
                .htmlContent("<p>Your OTP is <strong>[PROTECTED]</strong></p>")
                .actualOtp("123456")
                .build());

        assertThat(response.getStatus()).isEqualTo("SENT");
        assertThat(response.getContent()).contains("[PROTECTED]");
        assertThat(response.getContent()).doesNotContain("123456");
        assertThat(dumpMimeMessage()).contains("123456");

        ArgumentCaptor<NotificationLog> savedLogs = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, atLeastOnce()).save(savedLogs.capture());
        assertThat(savedLogs.getAllValues())
                .allSatisfy(saved -> assertThat(saved.getContent()).doesNotContain("123456"));
    }

    @Test
    void sendInternalEmail_missingProtectedValue_throwsBeforePersisting() {
        var request = InternalEmailRequest.builder()
                .userId("user-1")
                .recipientEmail("user@example.com")
                .type("FORGOT_PASSWORD_OTP")
                .content("Your OTP is [PROTECTED]")
                .build();

        assertThatThrownBy(() -> notificationService.sendInternalEmail(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Thieu gia tri");

        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    void sendInternalEmail_mailFailureStoresGenericError() {
        doThrow(new MailSendException("smtp-password-secret")).when(mailSender).send(any(MimeMessage.class));

        var response = notificationService.sendInternalEmail(InternalEmailRequest.builder()
                .userId("user-1")
                .recipientEmail("user@example.com")
                .type("USER_INVITE")
                .title("Invite")
                .content("Please join")
                .build());

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getErrorMessage()).isEqualTo("Mail delivery failed");
        assertThat(response.getErrorMessage()).doesNotContain("smtp-password-secret");
    }

    private NotificationLog notification(String userId, String status) {
        NotificationLog log = NotificationLog.builder()
                .id(1L)
                .userId(userId)
                .recipientEmail("user@example.com")
                .type("SYSTEM")
                .channel("EMAIL")
                .title("Hello")
                .content("World")
                .status(status)
                .build();
        log.setCreatedAt(LocalDateTime.now());
        log.setUpdatedAt(LocalDateTime.now());
        return log;
    }

    private String dumpMimeMessage() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mimeMessage.writeTo(out);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
