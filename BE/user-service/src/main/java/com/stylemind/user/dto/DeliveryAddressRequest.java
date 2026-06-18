package com.stylemind.user.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAddressRequest {
    @NotBlank(message = "Tên người nhận không được để trống")
    @Size(max = 100, message = "Tên người nhận tối đa 100 ký tự")
    private String recipientName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phoneNumber;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String addressLine;

    @NotBlank(message = "Thành phố không được để trống")
    @Size(max = 100, message = "Thành phố tối đa 100 ký tự")
    private String city;

    private Boolean isDefault = false;
}