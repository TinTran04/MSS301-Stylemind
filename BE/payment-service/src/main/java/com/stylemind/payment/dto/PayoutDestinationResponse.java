package com.stylemind.payment.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutDestinationResponse {
    private String returnRequestId;
    private String bankCode;
    private String accountHolder;
    private String maskedAccountNumber;
    private String status; // PROVIDED, MISSING
    private boolean editable;
    private LocalDateTime updatedAt;
}
