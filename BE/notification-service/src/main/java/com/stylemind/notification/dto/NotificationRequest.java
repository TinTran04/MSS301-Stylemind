package com.stylemind.notification.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {
    private String userId;

    @Email(message = "Email người nhận không hợp lệ")
    @Size(max = 150, message = "Email người nhận tối đa 150 ký tự")
    private String recipientEmail;

    @NotBlank(message = "Loại thông báo không được để trống")
    @Size(max = 30, message = "Loại thông báo tối đa 30 ký tự")
    private String type;

    @Builder.Default
    private String channel = "EMAIL";

    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
    private String title;

    private String content;

    @Size(max = 20, message = "Trạng thái tối đa 20 ký tự")
    private String status;

    private LocalDateTime sentAt;

    private Boolean sendEmail;

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public Boolean getSendEmail() { return sendEmail; }
    public void setSendEmail(Boolean sendEmail) { this.sendEmail = sendEmail; }
}
