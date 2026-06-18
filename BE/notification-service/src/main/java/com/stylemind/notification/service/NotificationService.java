package com.stylemind.notification.service;

import com.stylemind.notification.dto.*;
import com.stylemind.notification.entity.NotificationLog;
import com.stylemind.notification.repository.NotificationLogRepository;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;

    public NotificationResponse createNotification(NotificationRequest request) {
        NotificationLog log = NotificationLog.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .title(request.getTitle())
                .content(request.getContent())
                .status(request.getStatus())
                .sentAt(request.getSentAt() != null ? request.getSentAt() : LocalDateTime.now())
                .build();

        log = notificationLogRepository.save(log);
        return mapToResponse(log);
    }

    public List<NotificationResponse> getNotifications(String userId) {
        return notificationLogRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Page<NotificationResponse> getNotifications(String userId, Pageable pageable) {
        return notificationLogRepository.findByUserId(userId, pageable).map(this::mapToResponse);
    }

    public void markAsSent(Long id) {
        NotificationLog log = notificationLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOTIFICATION_NOT_FOUND", "Không tìm thấy thông báo", 404));
        
        log.setStatus("SENT");
        log.setSentAt(LocalDateTime.now());
        notificationLogRepository.save(log);
    }

    public void markAsFailed(Long id) {
        NotificationLog log = notificationLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOTIFICATION_NOT_FOUND", "Không tìm thấy thông báo", 404));
        
        log.setStatus("FAILED");
        notificationLogRepository.save(log);
    }

    private NotificationResponse mapToResponse(NotificationLog log) {
        return NotificationResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .type(log.getType())
                .title(log.getTitle())
                .content(log.getContent())
                .status(log.getStatus())
                .sentAt(log.getSentAt())
                .createdAt(log.getCreatedAt())
                .build();
    }
}