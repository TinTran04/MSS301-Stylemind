package com.stylemind.notification.dto;

import lombok.*;

/** Real notification health counts for the admin dashboard. Counts only. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminNotificationSummaryResponse {
    private long totalNotifications;
    private long sentNotifications;
    private long pendingNotifications;
    private long failedNotifications;
}
