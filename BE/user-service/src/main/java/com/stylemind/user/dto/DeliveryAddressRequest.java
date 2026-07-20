package com.stylemind.user.dto;

import com.stylemind.user.validation.ValidVietnameseLocation;
import com.stylemind.user.validation.ValidVietnamesePhone;
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
    @ValidVietnamesePhone
    private String phoneNumber;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String addressLine;

    @NotBlank(message = "Mã tỉnh/thành không được để trống")
    private String provinceCode;

    @NotBlank(message = "Mã phường/xã không được để trống")
    private String wardCode;

    @Size(max = 500, message = "Ghi chú giao hàng tối đa 500 ký tự")
    private String shippingNote;

    /** Legacy field accepted only during the migration window; it is not authoritative. */
    @Deprecated
    @ValidVietnameseLocation
    private String city;

    @Builder.Default
    private Boolean isDefault = false;
}
