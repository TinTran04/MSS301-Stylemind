package com.stylemind.notification.service;

import com.stylemind.notification.dto.InternalEmailRequest;
import com.stylemind.notification.dto.NotificationRequest;
import com.stylemind.notification.entity.NotificationLog;
import com.stylemind.notification.repository.NotificationLogRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationLogRepository notificationLogRepository;
    @Mock JavaMailSender mailSender;
    @Mock MimeMessage mimeMessage;

    @InjectMocks NotificationService notificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "mailEnabled", true);
        ReflectionTestUtils.setField(notificationService, "mailFrom", "no-reply@stylemind.ai");
        ReflectionTestUtils.setField(notificationService, "logFallback", true);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(notificationLogRepository.save(any())).thenAnswer(inv -> {
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
    void createNotification_withoutSendEmail_keepsPending() {
        var request = NotificationRequest.builder()
                .userId("user-1")
                .recipientEmail("user@example.com")
                .type("SYSTEM")
                .title("Hello")
                .content("World")
                .build();

        var response = notificationService.createNotification(request);

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getRecipientEmail()).isEqualTo("user@example.com");
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
}
