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
import java.util.List;
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
        ReflectionTestUtils.setField(notificationService, "fromAddress", "no-reply@stylemind.ai");
        ReflectionTestUtils.setField(notificationService, "fromName", "StyleMind");
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
    void sendInternalEmail_setsFromWithConfiguredDisplayName() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        org.mockito.ArgumentCaptor<jakarta.mail.Address> fromCaptor =
                org.mockito.ArgumentCaptor.forClass(jakarta.mail.Address.class);

        notificationService.sendInternalEmail(InternalEmailRequest.builder()
                .recipientEmail("user@example.com")
                .type("REGISTER_OTP")
                .title("OTP")
                .content("Your code is [PROTECTED]")
                .htmlContent("<b>123456</b>")
                .build());

        verify(mimeMessage).setFrom(fromCaptor.capture());
        var from = (jakarta.mail.internet.InternetAddress) fromCaptor.getValue();
        assertThat(from.getPersonal()).isEqualTo("StyleMind");
        assertThat(from.getAddress()).isEqualTo("no-reply@stylemind.ai");
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
    void getNotifications_returnsRepositoryNewestFirstOrder() {
        NotificationLog newest = NotificationLog.builder()
                .id(2L).userId("user-1").recipientEmail("user@example.com")
                .type("ORDER_CONFIRMED").channel("email").status("sent")
                .sentAt(LocalDateTime.of(2026, 7, 21, 9, 43))
                .build();
        NotificationLog older = NotificationLog.builder()
                .id(1L).userId("user-1").recipientEmail("user@example.com")
                .type("WELCOME").channel("email").status("sent")
                .sentAt(LocalDateTime.now().minusDays(1))
                .build();
        when(notificationLogRepository.findByUserIdNewestFirst("user-1")).thenReturn(List.of(newest, older));

        var response = notificationService.getNotifications("user-1");

        assertThat(response).extracting("id").containsExactly(2L, 1L);
        assertThat(response.get(0).getSentAt().toString()).isEqualTo("2026-07-21T09:43:00Z");
        verify(notificationLogRepository).findByUserIdNewestFirst("user-1");
    }

    @Test
    void getNotifications_translatesLegacyOrderNotificationCopy() {
        NotificationLog entry = NotificationLog.builder()
                .id(2L).userId("user-1").recipientEmail("user@example.com")
                .type("ORDER_CONFIRMED").channel("EMAIL").status("SENT")
                .title("Order confirmed")
                .content("Your order 42 has been confirmed and will be paid on delivery.")
                .sentAt(LocalDateTime.now())
                .build();
        when(notificationLogRepository.findByUserIdNewestFirst("user-1")).thenReturn(List.of(entry));

        var response = notificationService.getNotifications("user-1");

        assertThat(response.get(0).getTitle()).isEqualTo("Đơn hàng đã được xác nhận");
        assertThat(response.get(0).getContent())
                .isEqualTo("Đơn hàng #42 của bạn đã được xác nhận. Bạn sẽ thanh toán khi nhận hàng.");
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
