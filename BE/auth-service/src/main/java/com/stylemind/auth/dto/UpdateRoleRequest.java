package com.stylemind.auth.dto;

import com.stylemind.auth.entity.AccountRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateRoleRequest {

    @NotNull(message = "Role is required")
    private AccountRole role;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
