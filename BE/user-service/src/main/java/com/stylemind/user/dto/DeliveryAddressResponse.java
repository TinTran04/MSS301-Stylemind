package com.stylemind.user.dto;

import lombok.*;

import java.time.Instant;
import com.stylemind.user.entity.AddressValidationStatus;

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
    private String provinceCode;
    private String provinceName;
    private String wardCode;
    private String wardName;
    private String shippingNote;
    private AddressValidationStatus validationStatus;
    private String administrativeDataVersion;
    private String userId;
    private Boolean isDefault;
    private Instant createdAt;
    private Instant updatedAt;
}
