package com.stylemind.order.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnShipmentResponse {
    private String id;
    private String trackingCode;
    private String carrierName;
    private LocalDateTime shippedAt;
    private LocalDateTime receivedAt;
}
