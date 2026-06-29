package com.stylemind.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountStatusResponse {
    private String userId;
    private String accountStatus;
}
