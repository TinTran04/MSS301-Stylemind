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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    void getCustomerNotifications_translatesLegacyOrderNotificationCopy() {
        NotificationLog entry = NotificationLog.builder()
                .id(2L).userId("user-1").recipientEmail("user@example.com")
                .type("ORDER_CONFIRMED").channel("EMAIL").status("SENT")
                .title("Order confirmed")
                .content("Your order 42 has been confirmed and will be paid on delivery.")
                .sentAt(LocalDateTime.now())
                .build();
        var pageable = PageRequest.of(0, 10);
        when(notificationLogRepository.findCustomerPage("user-1", null, pageable))
                .thenReturn(new PageImpl<>(List.of(entry), pageable, 1));

        var response = notificationService.getCustomerNotifications("user-1", null, pageable);

        assertThat(response.getContent().get(0).getTitle()).isEqualTo("Đơn hàng đã được xác nhận");
        assertThat(response.getContent().get(0).getContent())
                .isEqualTo("Đơn hàng #42 của bạn đã được xác nhận. Bạn sẽ thanh toán khi nhận hàng.");
    }

    @Test
    void getCustomerNotifications_returnsPagedRepositoryResultForAuthenticatedUserAndReadFilter() {
        var pageable = PageRequest.of(1, 10);
        NotificationLog readEntry = NotificationLog.builder()
                .id(10L).userId("user-1").recipientEmail("user@example.com")
                .type("ORDER").channel("EMAIL").status("SENT")
                .readAt(LocalDateTime.of(2026, 7, 22, 10, 0))
                .build();
        readEntry.setCreatedAt(LocalDateTime.of(2026, 7, 22, 9, 30));
        when(notificationLogRepository.findCustomerPage("user-1", true, pageable))
                .thenReturn(new PageImpl<>(List.of(readEntry), pageable, 21));

        var response = notificationService.getCustomerNotifications("user-1", true, pageable);

        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(21);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.getNumberOfElements()).isEqualTo(1);
        assertThat(response.getContent()).extracting("id").containsExactly(10L);
        assertThat(response.getContent().get(0).isRead()).isTrue();
        assertThat(response.getContent().get(0).getReadAt().toString()).isEqualTo("2026-07-22T10:00:00Z");
        verify(notificationLogRepository).findCustomerPage("user-1", true, pageable);
    }

    @Test
    void getCustomerNotifications_unreadFilterMapsNullReadAtToUnread() {
        var pageable = PageRequest.of(0, 10);
        NotificationLog unreadEntry = NotificationLog.builder()
                .id(11L).userId("user-1").recipientEmail("user@example.com")
                .type("ORDER").channel("EMAIL").status("SENT")
                .build();
        when(notificationLogRepository.findCustomerPage("user-1", false, pageable))
                .thenReturn(new PageImpl<>(List.of(unreadEntry), pageable, 1));

        var response = notificationService.getCustomerNotifications("user-1", false, pageable);

        assertThat(response.getContent().get(0).isRead()).isFalse();
        assertThat(response.getContent().get(0).getReadAt()).isNull();
        verify(notificationLogRepository).findCustomerPage("user-1", false, pageable);
    }

    @Test
    void getUnreadCount_usesRepositoryCountQueryForAuthenticatedUser() {
        when(notificationLogRepository.countByUserIdAndReadAtIsNull("user-1")).thenReturn(7L);

        var response = notificationService.getUnreadCount("user-1");

        assertThat(response.getUnreadCount()).isEqualTo(7L);
        verify(notificationLogRepository).countByUserIdAndReadAtIsNull("user-1");
    }

    @Test
    void markNotificationRead_scopesLookupByUserAndPersistsReadAtWithoutChangingDeliveryStatus() {
        NotificationLog entry = NotificationLog.builder()
                .id(12L).userId("user-1").recipientEmail("user@example.com")
                .type("ORDER").channel("EMAIL").status("SENT")
                .build();
        when(notificationLogRepository.findByIdAndUserId(12L, "user-1")).thenReturn(Optional.of(entry));

        var response = notificationService.markNotificationRead("user-1", 12L);

        assertThat(entry.getReadAt()).isNotNull();
        assertThat(response.isRead()).isTrue();
        assertThat(response.getStatus()).isEqualTo("SENT");
        verify(notificationLogRepository).findByIdAndUserId(12L, "user-1");
        verify(notificationLogRepository).save(entry);
    }

    @Test
    void markNotificationRead_ownedBySomeoneElseThrowsNotFound() {
        when(notificationLogRepository.findByIdAndUserId(13L, "user-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markNotificationRead("user-2", 13L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "NOTIFICATION_NOT_FOUND");
    }

    @Test
    void markAllNotificationsRead_updatesOnlyUnreadRowsForAuthenticatedUser() {
        when(notificationLogRepository.markAllReadByUserId(eq("user-1"), any(LocalDateTime.class))).thenReturn(5);

        var response = notificationService.markAllNotificationsRead("user-1");

        assertThat(response.getUpdatedCount()).isEqualTo(5);
        verify(notificationLogRepository).markAllReadByUserId(eq("user-1"), any(LocalDateTime.class));
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
