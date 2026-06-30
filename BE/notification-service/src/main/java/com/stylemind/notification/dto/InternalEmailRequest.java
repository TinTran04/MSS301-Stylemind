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

    @Size(max = 50, message = "ID nguoi dung toi da 50 ky tu")
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

    @Size(max = 10000, message = "Noi dung toi da 10000 ky tu")
    private String content;

    @Size(max = 10000, message = "Noi dung HTML toi da 10000 ky tu")
    private String htmlContent;

    /** Sensitive delivery value for templates; never persisted or logged. */
    private String actualOtp;
}
