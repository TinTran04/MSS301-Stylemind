package com.stylemind.auth.dto;

import lombok.*;

/** Real user counts for the admin dashboard. Counts only — no PII/credentials. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserSummaryResponse {
    private long totalUsers;
    private long totalCustomers;
    private long totalAdmins;
}
