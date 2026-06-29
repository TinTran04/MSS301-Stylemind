package com.stylemind.auth.dto;

import com.stylemind.auth.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAccountStatusRequest {

    @NotNull(message = "Account status is required")
    private AccountStatus accountStatus;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
