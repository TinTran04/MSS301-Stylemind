package com.stylemind.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeEnabledRequest {
    @NotNull
    private Boolean enabled;
}
