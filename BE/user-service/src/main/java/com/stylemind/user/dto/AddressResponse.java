package com.stylemind.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressResponse {
    private String addressId;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String district;
    private String ward;
    private String streetAddress;
    @JsonProperty("isDefault")
    private boolean isDefault;
    private Instant createdAt;
    private Instant updatedAt;
}
