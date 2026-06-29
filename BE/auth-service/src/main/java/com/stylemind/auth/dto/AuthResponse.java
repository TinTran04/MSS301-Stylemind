package com.stylemind.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private UserResponse user;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginResponse {
        private String accessToken;
        private String tokenType;
        private long expiresInSeconds;
        @JsonIgnore
        private String refreshToken;
        @JsonIgnore
        private long refreshExpiresInSeconds;
        private UserResponse user;
    }
}
