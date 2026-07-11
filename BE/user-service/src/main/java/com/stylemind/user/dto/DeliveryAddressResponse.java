package com.stylemind.user.dto;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAddressResponse {
    private String id;
    private String recipientName;
    private String phoneNumber;
    private String addressLine;
    private String city;
    private Boolean isDefault;
    private Instant createdAt;
    private Instant updatedAt;
}