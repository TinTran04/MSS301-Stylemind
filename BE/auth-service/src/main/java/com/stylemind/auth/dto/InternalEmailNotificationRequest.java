package com.stylemind.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalEmailNotificationRequest {
    private String userId;
    private String recipientEmail;
    private String type;
    private String title;
    @ToString.Exclude
    private String content;
    @ToString.Exclude
    private String htmlContent;
}
