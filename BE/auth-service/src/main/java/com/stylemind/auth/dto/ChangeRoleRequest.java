package com.stylemind.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangeRoleRequest {
    @NotBlank
    @Pattern(regexp = "CUSTOMER|ADMIN", message = "Role must be CUSTOMER or ADMIN")
    private String role;
}
