package com.stylemind.auth.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterResponse {
    private String id;
    private String email;
    private String role;
    private String status;
    private boolean emailVerified;
    private Instant createdAt;
}
