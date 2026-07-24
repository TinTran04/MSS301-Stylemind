package com.stylemind.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitShipmentRequest {

    @NotBlank(message = "TRACKING_CODE_REQUIRED")
    private String trackingCode;

    private String carrierName;
}
