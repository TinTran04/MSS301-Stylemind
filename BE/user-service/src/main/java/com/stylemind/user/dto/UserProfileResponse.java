package com.stylemind.user.dto;

import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private String userId;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private String gender;
    private LocalDate dateOfBirth;
    private Instant createdAt;
    private Instant updatedAt;
}
