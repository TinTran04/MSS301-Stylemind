package com.stylemind.auth.dto;

import com.stylemind.auth.entity.AccountStatus;
import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserResponse {
    private String id;
    private String email;
    private String role;
    private String provider;
    private AccountStatus accountStatus;
    private Boolean enabled;
    private Boolean passwordSetupRequired;
    private Instant createdAt;
    private Instant updatedAt;
}
