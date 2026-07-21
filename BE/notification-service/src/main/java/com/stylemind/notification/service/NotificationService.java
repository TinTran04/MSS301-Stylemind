package com.stylemind.notification.service;

import com.stylemind.notification.dto.*;
import com.stylemind.notification.entity.NotificationLog;
import com.stylemind.notification.repository.NotificationLogRepository;
import com.stylemind.common.constant.ErrorCode;
import com.stylemind.common.dto.PageResponse;
import com.stylemind.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    // Sender address defaults to the authenticated SMTP account so Gmail doesn't
    // rewrite the From header; no personal address is hardcoded here.
    @Value("${app.mail.from-address:${spring.mail.username:no-reply@stylemind.ai}}")
    private String fromAddress;

    // Display name shown to recipients, e.g. "StyleMind <account@gmail.com>".
    @Value("${app.mail.from-name:StyleMind}")
    private String fromName;

    @Value("${app.mail.log-fallback:true}")
    private boolean logFallback;

    @Autowired
    public NotificationService(NotificationLogRepository notificationLogRepository,
                              @Autowired(required = false) JavaMailSender mailSender) {
        this.notificationLogRepository = notificationLogRepository;
        this.mailSender = mailSender;
    }

    public NotificationResponse createNotification(NotificationRequest request) {
        NotificationLog entry = NotificationLog.builder()
                .userId(request.getUserId())
                .recipientEmail(request.getRecipientEmail())
                .type(normalizeType(request.getType()))
                .channel(normalizeChannel(request.getChannel()))
                .title(request.getTitle())
                .content(request.getContent())
                .status(defaultStatus(request.getStatus()))
                .sentAt(request.getSentAt())
                .build();

        entry = notificationLogRepository.save(entry);
        if (Boolean.TRUE.equals(request.getSendEmail()) && StringUtils.hasText(entry.getRecipientEmail())) {
            entry = sendEmail(entry, request.getContent());
        }
        return mapToResponse(entry);
    }

    public PageResponse<NotificationResponse> getCustomerNotifications(String userId, Boolean read, Pageable pageable) {
        return PageResponse.of(notificationLogRepository.findCustomerPage(userId, read, pageable)
                .map(this::mapToResponse));
    }

    public NotificationUnreadCountResponse getUnreadCount(String userId) {
        return NotificationUnreadCountResponse.builder()
                .unreadCount(notificationLogRepository.countByUserIdAndReadAtIsNull(userId))
                .build();
    }

    public NotificationResponse getNotificationForUser(String userId, Long id) {
        NotificationLog entry = notificationLogRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        return mapToResponse(entry);
    }

    public Page<NotificationResponse> getNotifications(String userId, String status, String type, Pageable pageable) {
        return notificationLogRepository.search(
                normalizeNullable(userId),
                normalizeStatusFilter(status),
                normalizeTypeFilter(type),
                pageable
        ).map(this::mapToResponse);
    }

    /** Real notification-health counts for the admin dashboard. */
    @Transactional(readOnly = true)
    public AdminNotificationSummaryResponse getAdminSummary() {
        return AdminNotificationSummaryResponse.builder()
                .totalNotifications(notificationLogRepository.count())
                .sentNotifications(notificationLogRepository.countByStatus("SENT"))
                .pendingNotifications(notificationLogRepository.countByStatus("PENDING"))
                .failedNotifications(notificationLogRepository.countByStatus("FAILED"))
                .build();
    }

    // Admin-triggered resend of a previously FAILED notification. Only FAILED
    // entries are retryable - PENDING/SENT/SKIPPED already reflect a final or
    // in-progress outcome that a blind resend would misrepresent.
    public NotificationResponse retryNotification(Long id) {
        NotificationLog entry = notificationLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!"FAILED".equals(entry.getStatus()) || !StringUtils.hasText(entry.getRecipientEmail())) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_RETRYABLE);
        }
        entry = sendEmail(entry, entry.getContent());
        return mapToResponse(entry);
    }

    public NotificationResponse sendInternalEmail(InternalEmailRequest request) {
        NotificationLog entry = NotificationLog.builder()
                .userId(request.getUserId())
                .recipientEmail(request.getRecipientEmail())
                .type(normalizeType(request.getType()))
                .channel("EMAIL")
                .title(request.getTitle())
                .content(request.getContent())
                .status("PENDING")
                .build();
        entry = notificationLogRepository.save(entry);
        entry = sendEmail(entry, request.getHtmlContent());
        return mapToResponse(entry);
    }

    public NotificationResponse markNotificationRead(String userId, Long id) {
        NotificationLog entry = notificationLogRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (entry.getReadAt() == null) {
            entry.setReadAt(LocalDateTime.now());
            entry = notificationLogRepository.save(entry);
        }
        return mapToResponse(entry);
    }

    public NotificationReadAllResponse markAllNotificationsRead(String userId) {
        return NotificationReadAllResponse.builder()
                .updatedCount(notificationLogRepository.markAllReadByUserId(userId, LocalDateTime.now()))
                .build();
    }

    private NotificationResponse mapToResponse(NotificationLog entry) {
        return NotificationResponse.builder()
                .id(entry.getId())
                .userId(entry.getUserId())
                .recipientEmail(entry.getRecipientEmail())
                .type(entry.getType())
                .channel(entry.getChannel())
                .title(resolveDisplayTitle(entry))
                .content(resolveDisplayContent(entry))
                .status(entry.getStatus())
                .errorMessage(entry.getErrorMessage())
                .sentAt(toUtcInstant(entry.getSentAt()))
                .readAt(toUtcInstant(entry.getReadAt()))
                .read(entry.getReadAt() != null)
                .createdAt(toUtcInstant(entry.getCreatedAt()))
                .build();
    }

    private Instant toUtcInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private String resolveDisplayTitle(NotificationLog entry) {
        String title = entry.getTitle();
        if ("Order confirmed".equals(title)) {
            return "Đơn hàng đã được xác nhận";
        }
        if ("Payment received".equals(title)) {
            return "Thanh toán thành công";
        }
        return title;
    }

    private String resolveDisplayContent(NotificationLog entry) {
        String content = entry.getContent();
        if (!StringUtils.hasText(content)) {
            return content;
        }

        String confirmedOrderId = extractBetween(content, "Your order ", " has been confirmed");
        if (confirmedOrderId != null && ("ORDER_CONFIRMED".equals(entry.getType()) || "Order confirmed".equals(entry.getTitle()))) {
            return "Đơn hàng " + formatOrderReference(confirmedOrderId)
                    + " của bạn đã được xác nhận. Bạn sẽ thanh toán khi nhận hàng.";
        }

        String paidOrderId = extractBetween(content, "Payment for your order ", " has been received");
        if (paidOrderId != null && ("ORDER_PAID".equals(entry.getType()) || "Payment received".equals(entry.getTitle()))) {
            return "Thanh toán cho đơn hàng " + formatOrderReference(paidOrderId)
                    + " đã được ghi nhận thành công.";
        }

        return content;
    }

    private String extractBetween(String value, String prefix, String suffix) {
        int start = value.indexOf(prefix);
        if (start < 0) {
            return null;
        }
        start += prefix.length();
        int end = value.indexOf(suffix, start);
        if (end < 0 || end <= start) {
            return null;
        }
        String extracted = value.substring(start, end).trim();
        return StringUtils.hasText(extracted) ? extracted : null;
    }

    private String formatOrderReference(String orderId) {
        return orderId.startsWith("#") ? orderId : "#" + orderId;
    }

    private NotificationLog sendEmail(NotificationLog entry, String htmlContent) {
        if (!mailEnabled || mailSender == null) {
            if (logFallback) {
                log.info("MAIL_DISABLED fallback for {}: {}", entry.getRecipientEmail(), entry.getContent());
            }
            entry.setStatus("SKIPPED");
            entry.setErrorMessage("Mail disabled");
            return notificationLogRepository.save(entry);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            // Set both address and display name → recipients see "StyleMind <address>".
            helper.setFrom(fromAddress, fromName);
            helper.setTo(entry.getRecipientEmail());
            helper.setSubject(StringUtils.hasText(entry.getTitle()) ? entry.getTitle() : "StyleMind notification");
            if (StringUtils.hasText(htmlContent)) {
                helper.setText(htmlContent, true);
            } else {
                helper.setText(entry.getContent(), false);
            }
            mailSender.send(message);
            entry.setStatus("SENT");
            entry.setSentAt(LocalDateTime.now());
            entry.setErrorMessage(null);
        } catch (MailException | jakarta.mail.MessagingException | java.io.UnsupportedEncodingException ex) {
            log.warn("Failed to send email to {}: {}", entry.getRecipientEmail(), ex.getMessage());
            entry.setStatus("FAILED");
            entry.setErrorMessage(ex.getMessage());
        }

        return notificationLogRepository.save(entry);
    }

    private String defaultStatus(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase() : "PENDING";
    }

    private String normalizeChannel(String channel) {
        return StringUtils.hasText(channel) ? channel.trim().toUpperCase() : "EMAIL";
    }

    private String normalizeType(String type) {
        return StringUtils.hasText(type) ? type.trim().toUpperCase() : type;
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeStatusFilter(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase() : null;
    }

    private String normalizeTypeFilter(String type) {
        return StringUtils.hasText(type) ? type.trim().toUpperCase() : null;
    }
}
