package com.stylemind.auth.dto;

import com.stylemind.auth.entity.AccountStatus;
import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private String id;
    private String email;
    private String role;
    private String provider;
    private AccountStatus accountStatus;
    private Boolean enabled;
    private Instant createdAt;
}
