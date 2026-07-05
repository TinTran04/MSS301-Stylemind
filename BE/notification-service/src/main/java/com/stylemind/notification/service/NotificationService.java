package com.stylemind.notification.service;

import com.stylemind.notification.dto.*;
import com.stylemind.notification.entity.NotificationLog;
import com.stylemind.notification.repository.NotificationLogRepository;
import com.stylemind.common.constant.ErrorCode;
import com.stylemind.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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

    public NotificationResponse createNotification(NotificationRequest request) {
        NotificationLog entry = NotificationLog.builder()
                .userId(request.getUserId())
                .recipientEmail(request.getRecipientEmail())
                .type(request.getType())
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

    public List<NotificationResponse> getNotifications(String userId) {
        return notificationLogRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public NotificationResponse getNotificationForUser(String userId, Long id) {
        NotificationLog entry = notificationLogRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        return mapToResponse(entry);
    }

    public Page<NotificationResponse> getNotifications(String userId, String status, String type, Pageable pageable) {
        return notificationLogRepository.search(userId, status, type, pageable).map(this::mapToResponse);
    }

    /** Real notification-health counts for the admin dashboard. */
    @Transactional(readOnly = true)
    public AdminNotificationSummaryResponse getAdminSummary() {
        return AdminNotificationSummaryResponse.builder()
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
                .type(request.getType())
                .channel("EMAIL")
                .title(request.getTitle())
                .content(request.getContent())
                .status("PENDING")
                .build();
        entry = notificationLogRepository.save(entry);
        entry = sendEmail(entry, request.getHtmlContent());
        return mapToResponse(entry);
    }

    private NotificationResponse mapToResponse(NotificationLog entry) {
        return NotificationResponse.builder()
                .id(entry.getId())
                .userId(entry.getUserId())
                .recipientEmail(entry.getRecipientEmail())
                .type(entry.getType())
                .channel(entry.getChannel())
                .title(entry.getTitle())
                .content(entry.getContent())
                .status(entry.getStatus())
                .errorMessage(entry.getErrorMessage())
                .sentAt(entry.getSentAt())
                .createdAt(entry.getCreatedAt())
                .build();
    }

    private NotificationLog sendEmail(NotificationLog entry, String htmlContent) {
        if (!mailEnabled) {
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
        return StringUtils.hasText(status) ? status : "PENDING";
    }

    private String normalizeChannel(String channel) {
        return StringUtils.hasText(channel) ? channel.toUpperCase() : "EMAIL";
    }
}
