package com.stylemind.auth.dto;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserResponse {
    private String id;
    private String email;
    private String fullName;
    private String role;
    private String provider;
    private Boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
}
