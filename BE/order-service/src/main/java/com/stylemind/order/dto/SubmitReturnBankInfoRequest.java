package com.stylemind.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitReturnBankInfoRequest {
    @NotBlank
    @Size(max = 120)
    private String bankName;

    @NotBlank
    @Size(max = 80)
    private String bankAccountNumber;

    @NotBlank
    @Size(max = 150)
    private String bankAccountHolder;

    @Size(max = 150)
    private String bankBranch;
}
