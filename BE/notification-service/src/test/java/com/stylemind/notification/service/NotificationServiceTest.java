package com.stylemind.notification.service;

import com.stylemind.common.exception.BusinessException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

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
    void getNotificationForUser_ownedByCaller_returnsIt() {
        NotificationLog entry = NotificationLog.builder()
                .id(1L).userId("user-1").recipientEmail("user@example.com")
                .type("ORDER").channel("EMAIL").status("SENT")
                .build();
        when(notificationLogRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(entry));

        var response = notificationService.getNotificationForUser("user-1", 1L);

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void getNotificationForUser_ownedBySomeoneElse_throwsNotFound() {
        when(notificationLogRepository.findByIdAndUserId(1L, "user-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getNotificationForUser("user-2", 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "NOTIFICATION_NOT_FOUND");
    }

    @Test
    void retryNotification_failedWithEmail_resendsAndMarksSent() {
        NotificationLog entry = NotificationLog.builder()
                .id(2L).userId("user-1").recipientEmail("user@example.com")
                .type("ORDER").channel("EMAIL").title("Order update").content("Your order shipped")
                .status("FAILED")
                .build();
        when(notificationLogRepository.findById(2L)).thenReturn(Optional.of(entry));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        var response = notificationService.retryNotification(2L);

        assertThat(response.getStatus()).isEqualTo("SENT");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void retryNotification_notFailed_throwsNotRetryable() {
        NotificationLog entry = NotificationLog.builder()
                .id(3L).userId("user-1").recipientEmail("user@example.com")
                .type("ORDER").channel("EMAIL").status("SENT")
                .build();
        when(notificationLogRepository.findById(3L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> notificationService.retryNotification(3L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "NOTIFICATION_NOT_RETRYABLE");
    }

    @Test
    void retryNotification_failedWithoutRecipientEmail_throwsNotRetryable() {
        NotificationLog entry = NotificationLog.builder()
                .id(4L).userId("user-1").recipientEmail(null)
                .type("ORDER").channel("EMAIL").status("FAILED")
                .build();
        when(notificationLogRepository.findById(4L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> notificationService.retryNotification(4L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "NOTIFICATION_NOT_RETRYABLE");
    }
}
