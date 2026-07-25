package com.stylemind.notification.service;

import com.stylemind.common.dto.PageResponse;
import com.stylemind.notification.dto.AdminNotificationSummaryResponse;
import com.stylemind.notification.dto.InternalEmailRequest;
import com.stylemind.notification.dto.NotificationReadAllResponse;
import com.stylemind.notification.dto.NotificationRequest;
import com.stylemind.notification.dto.NotificationResponse;
import com.stylemind.notification.dto.NotificationUnreadCountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    NotificationResponse createNotification(NotificationRequest request);

    PageResponse<NotificationResponse> getCustomerNotifications(String userId, Boolean read, Pageable pageable);

    NotificationUnreadCountResponse getUnreadCount(String userId);

    NotificationResponse getNotificationForUser(String userId, Long id);

    Page<NotificationResponse> getNotifications(String userId, String status, String type, Pageable pageable);

    AdminNotificationSummaryResponse getAdminSummary();

    NotificationResponse retryNotification(Long id);

    NotificationResponse sendInternalEmail(InternalEmailRequest request);

    NotificationResponse markNotificationRead(String userId, Long id);

    NotificationReadAllResponse markAllNotificationsRead(String userId);
}
