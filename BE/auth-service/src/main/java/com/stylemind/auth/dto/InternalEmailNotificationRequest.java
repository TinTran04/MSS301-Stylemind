package com.stylemind.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalEmailNotificationRequest {
    private String userId;
    private String recipientEmail;
    private String type;
    private String title;
    private String content;
    private String htmlContent;
    /** Raw OTP/token for email embedding — never log this field. */
    private String actualOtp;
}

