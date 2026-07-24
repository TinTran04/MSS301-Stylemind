package com.stylemind.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutDestinationRequest {

    @NotBlank(message = "BANK_CODE_REQUIRED")
    private String bankCode;

    @NotBlank(message = "ACCOUNT_HOLDER_REQUIRED")
    private String accountHolder;

    @NotBlank(message = "ACCOUNT_NUMBER_REQUIRED")
    private String accountNumber;
}
