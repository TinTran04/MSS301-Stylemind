package com.stylemind.user.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileRequest {

    @Size(max = 150, message = "Full name must not exceed 150 characters")
    private String fullName;

    @Pattern(regexp = "^$|^[0-9+() .-]{7,20}$", message = "Phone format is invalid")
    private String phone;

    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    private String avatarUrl;

    @Size(max = 30, message = "Gender must not exceed 30 characters")
    private String gender;

    @PastOrPresent(message = "Date of birth must not be in the future")
    private LocalDate dateOfBirth;

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
