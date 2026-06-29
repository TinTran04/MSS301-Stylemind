package com.stylemind.user.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequest {

    @NotBlank(message = "Receiver name is required")
    @Size(max = 150, message = "Receiver name must not exceed 150 characters")
    private String receiverName;

    @NotBlank(message = "Receiver phone is required")
    @Pattern(regexp = "^[0-9+() .-]{7,20}$", message = "Receiver phone format is invalid")
    private String receiverPhone;

    @NotBlank(message = "Province is required")
    @Size(max = 100, message = "Province must not exceed 100 characters")
    private String province;

    @NotBlank(message = "District is required")
    @Size(max = 100, message = "District must not exceed 100 characters")
    private String district;

    @NotBlank(message = "Ward is required")
    @Size(max = 100, message = "Ward must not exceed 100 characters")
    private String ward;

    @NotBlank(message = "Street address is required")
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
}
