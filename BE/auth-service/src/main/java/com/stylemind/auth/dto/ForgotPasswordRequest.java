package com.stylemind.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    @Size(max = 320, message = "Email must not exceed 320 characters")
    private String email;

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }
}
