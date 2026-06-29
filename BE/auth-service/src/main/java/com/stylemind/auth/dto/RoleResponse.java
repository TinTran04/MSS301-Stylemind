package com.stylemind.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleResponse {
    private String userId;
    private String role;
}
