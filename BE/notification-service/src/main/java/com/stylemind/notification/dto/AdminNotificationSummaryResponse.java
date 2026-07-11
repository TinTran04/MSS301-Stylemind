package com.stylemind.notification.dto;

import lombok.*;

/** Real notification health counts for the admin dashboard. Counts only. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminNotificationSummaryResponse {
    private long failedNotifications;
}
