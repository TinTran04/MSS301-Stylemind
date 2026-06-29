package com.stylemind.user.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressPatchRequest {

    @Size(max = 150, message = "Receiver name must not exceed 150 characters")
    private String receiverName;

    @Pattern(regexp = "^$|^[0-9+() .-]{7,20}$", message = "Receiver phone format is invalid")
    private String receiverPhone;

    @Size(max = 100, message = "Province must not exceed 100 characters")
    private String province;

    @Size(max = 100, message = "District must not exceed 100 characters")
    private String district;

    @Size(max = 100, message = "Ward must not exceed 100 characters")
    private String ward;

    @Size(max = 500, message = "Street address must not exceed 500 characters")
    private String streetAddress;

    private Boolean isDefault;

    private final Map<String, Object> unknownFields = new LinkedHashMap<>();

    @JsonAnySetter
    void captureUnknownField(String fieldName, Object value) {
        unknownFields.put(fieldName, value);
    }

    @AssertTrue(message = "Request contains unsupported fields")
    public boolean hasNoUnsupportedFields() {
        return unknownFields.isEmpty();
    }

    @AssertTrue(message = "Address fields must not be blank")
    public boolean hasNoBlankAddressFields() {
        return isNullOrNotBlank(receiverName)
                && isNullOrNotBlank(receiverPhone)
                && isNullOrNotBlank(province)
                && isNullOrNotBlank(district)
                && isNullOrNotBlank(ward)
                && isNullOrNotBlank(streetAddress);
    }

    private boolean isNullOrNotBlank(String value) {
        return value == null || !value.isBlank();
    }
}
