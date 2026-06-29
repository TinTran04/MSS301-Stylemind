package com.stylemind.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalEmailRequest {

    private String userId;

    @NotBlank(message = "Email người nhận không được để trống")
    @Email(message = "Email người nhận không hợp lệ")
    @Size(max = 150, message = "Email người nhận tối đa 150 ký tự")
    private String recipientEmail;

    @NotBlank(message = "Loại thông báo không được để trống")
    @Size(max = 30, message = "Loại thông báo tối đa 30 ký tự")
    private String type;

    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
    private String title;

    private String content;

    private String htmlContent;
}
