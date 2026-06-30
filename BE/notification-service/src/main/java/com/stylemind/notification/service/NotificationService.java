package com.stylemind.notification.service;

import com.stylemind.common.constant.ErrorCode;
import com.stylemind.common.dto.PageResponse;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.notification.dto.InternalEmailRequest;
import com.stylemind.notification.dto.NotificationRequest;
import com.stylemind.notification.dto.NotificationResponse;
import com.stylemind.notification.entity.NotificationLog;
import com.stylemind.notification.repository.NotificationLogRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private static final Set<String> ALLOWED_CHANNELS = Set.of("EMAIL", "IN_APP");
    private static final Set<String> ALLOWED_STATUSES = Set.of("PENDING", "SENT", "FAILED", "SKIPPED");
    private static final Pattern TYPE_PATTERN = Pattern.compile("^[A-Z0-9_]+$");
    private static final Pattern SENSITIVE_QUERY_PARAM_PATTERN =
            Pattern.compile("(?i)([?&](?:token|otp|resetToken|setupToken)=)[^\\s&\"'<>]+");
    private static final String PROTECTED_PLACEHOLDER = "[PROTECTED]";
    private static final String MAIL_DELIVERY_FAILED = "Mail delivery failed";

    private final NotificationLogRepository notificationLogRepository;
    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@stylemind.ai}")
    private String mailFrom;

    @Value("${app.mail.log-fallback:true}")
    private boolean logFallback;

    public NotificationResponse createNotification(NotificationRequest request) {
        String channel = normalizeChannel(request.getChannel());
        String status = normalizeStatus(defaultStatus(request.getStatus()));

        if (Boolean.TRUE.equals(request.getSendEmail())) {
            validateEmailDelivery(channel, request.getRecipientEmail());
        }

        NotificationLog entry = buildEntry(request, channel, status, sanitizeSensitiveContent(request.getContent()));
        entry = notificationLogRepository.save(entry);

        if (Boolean.TRUE.equals(request.getSendEmail())) {
            entry = sendEmail(entry, null, entry.getContent());
        }
        return mapToAdminResponse(entry);
    }

    public NotificationResponse createNotificationLog(NotificationRequest request) {
        NotificationLog entry = buildEntry(
                request,
                normalizeChannel(request.getChannel()),
                normalizeStatus(defaultStatus(request.getStatus())),
                sanitizeSensitiveContent(request.getContent()));
        return mapToAdminResponse(notificationLogRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getUserNotifications(String userId, Pageable pageable) {
        return PageResponse.of(notificationLogRepository.findByUserId(userId, pageable)
                .map(this::mapToCustomerResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getAdminNotifications(
            String userId, String status, String type, Pageable pageable) {
        return PageResponse.of(notificationLogRepository.search(
                        blankToNull(userId),
                        normalizeOptionalStatus(status),
                        normalizeOptionalType(type),
                        pageable)
                .map(this::mapToAdminResponse));
    }

    public NotificationResponse sendInternalEmail(InternalEmailRequest request) {
        String deliveryHtmlContent = applyProtectedTemplateValue(request.getHtmlContent(), request.getActualOtp());
        String deliveryTextContent = applyProtectedTemplateValue(request.getContent(), request.getActualOtp());

        validateEmailDelivery("EMAIL", request.getRecipientEmail());

        NotificationLog entry = NotificationLog.builder()
                .userId(blankToNull(request.getUserId()))
                .recipientEmail(request.getRecipientEmail().trim())
                .type(normalizeType(request.getType()))
                .channel("EMAIL")
                .title(trimToNull(request.getTitle()))
                .content(sanitizeSensitiveContent(request.getContent()))
                .status("PENDING")
                .build();
        entry = notificationLogRepository.save(entry);
        entry = sendEmail(entry, deliveryHtmlContent, deliveryTextContent);
        return mapToAdminResponse(entry);
    }

    public void markAsSent(Long id) {
        NotificationLog entry = findById(id);
        entry.setStatus("SENT");
        entry.setSentAt(LocalDateTime.now());
        entry.setErrorMessage(null);
        notificationLogRepository.save(entry);
    }

    public void markAsFailed(Long id) {
        NotificationLog entry = findById(id);
        entry.setStatus("FAILED");
        notificationLogRepository.save(entry);
    }

    private NotificationLog buildEntry(NotificationRequest request, String channel, String status, String content) {
        return NotificationLog.builder()
                .userId(blankToNull(request.getUserId()))
                .recipientEmail(trimToNull(request.getRecipientEmail()))
                .type(normalizeType(request.getType()))
                .channel(channel)
                .title(trimToNull(request.getTitle()))
                .content(content)
                .status(status)
                .sentAt(request.getSentAt())
                .build();
    }

    private NotificationLog findById(Long id) {
        return notificationLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    private NotificationResponse mapToAdminResponse(NotificationLog entry) {
        return mapToResponse(entry, true);
    }

    private NotificationResponse mapToCustomerResponse(NotificationLog entry) {
        return mapToResponse(entry, false);
    }

    private NotificationResponse mapToResponse(NotificationLog entry, boolean includeErrorMessage) {
        return NotificationResponse.builder()
                .id(entry.getId())
                .userId(entry.getUserId())
                .recipientEmail(entry.getRecipientEmail())
                .type(entry.getType())
                .channel(entry.getChannel())
                .title(entry.getTitle())
                .content(entry.getContent())
                .status(entry.getStatus())
                .errorMessage(includeErrorMessage ? entry.getErrorMessage() : null)
                .sentAt(entry.getSentAt())
                .createdAt(entry.getCreatedAt())
                .build();
    }

    private NotificationLog sendEmail(NotificationLog entry, String htmlContent, String textContent) {
        if (!mailEnabled) {
            if (logFallback) {
                log.info(
                        "MAIL_DISABLED fallback notificationId={} type={} recipient={}",
                        entry.getId(),
                        entry.getType(),
                        entry.getRecipientEmail());
            }
            entry.setStatus("SKIPPED");
            entry.setErrorMessage("Mail disabled");
            return notificationLogRepository.save(entry);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(entry.getRecipientEmail());
            helper.setSubject(StringUtils.hasText(entry.getTitle()) ? entry.getTitle() : "StyleMind notification");
            if (StringUtils.hasText(htmlContent)) {
                helper.setText(htmlContent, true);
            } else {
                helper.setText(StringUtils.hasText(textContent) ? textContent : "", false);
            }
            mailSender.send(message);
            entry.setStatus("SENT");
            entry.setSentAt(LocalDateTime.now());
            entry.setErrorMessage(null);
        } catch (MailException | MessagingException ex) {
            log.warn(
                    "Failed to send email notificationId={} type={} recipient={} cause={}",
                    entry.getId(),
                    entry.getType(),
                    entry.getRecipientEmail(),
                    ex.getClass().getSimpleName());
            entry.setStatus("FAILED");
            entry.setErrorMessage(MAIL_DELIVERY_FAILED);
        }

        return notificationLogRepository.save(entry);
    }

    private String defaultStatus(String status) {
        return StringUtils.hasText(status) ? status : "PENDING";
    }

    private String normalizeOptionalStatus(String status) {
        return StringUtils.hasText(status) ? normalizeStatus(status) : null;
    }

    private String normalizeStatus(String status) {
        String normalized = normalizeToken(status);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new BusinessException("INVALID_NOTIFICATION_STATUS", "Trang thai thong bao khong hop le", 400);
        }
        return normalized;
    }

    private String normalizeOptionalType(String type) {
        return StringUtils.hasText(type) ? normalizeType(type) : null;
    }

    private String normalizeType(String type) {
        String normalized = normalizeToken(type);
        if (!TYPE_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException("INVALID_NOTIFICATION_TYPE", "Loai thong bao khong hop le", 400);
        }
        return normalized;
    }

    private String normalizeChannel(String channel) {
        String normalized = StringUtils.hasText(channel) ? normalizeToken(channel) : "EMAIL";
        if (!ALLOWED_CHANNELS.contains(normalized)) {
            throw new BusinessException("INVALID_NOTIFICATION_CHANNEL", "Kenh thong bao khong hop le", 400);
        }
        return normalized;
    }

    private String normalizeToken(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private void validateEmailDelivery(String channel, String recipientEmail) {
        if (!"EMAIL".equals(channel)) {
            throw new BusinessException("INVALID_NOTIFICATION_CHANNEL", "Gui email chi ho tro kenh EMAIL", 400);
        }
        if (!StringUtils.hasText(recipientEmail)) {
            throw new BusinessException("RECIPIENT_EMAIL_REQUIRED", "Email nguoi nhan la bat buoc", 400);
        }
    }

    private String applyProtectedTemplateValue(String content, String protectedValue) {
        if (!StringUtils.hasText(content) || !content.contains(PROTECTED_PLACEHOLDER)) {
            return content;
        }
        if (!StringUtils.hasText(protectedValue)) {
            throw new BusinessException(
                    "NOTIFICATION_TEMPLATE_VALUE_MISSING",
                    "Thieu gia tri bao mat cho mau email",
                    400);
        }
        return content.replace(PROTECTED_PLACEHOLDER, protectedValue);
    }

    private String sanitizeSensitiveContent(String content) {
        if (!StringUtils.hasText(content)) {
            return content;
        }
        return SENSITIVE_QUERY_PARAM_PATTERN.matcher(content).replaceAll("$1" + PROTECTED_PLACEHOLDER);
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String trimToNull(String value) {
        return blankToNull(value);
    }
}
