package com.stylemind.user.controller;

import com.stylemind.user.dto.DeliveryAddressRequest;
import com.stylemind.user.dto.DeliveryAddressResponse;
import com.stylemind.user.dto.StyleProfileRequest;
import com.stylemind.user.dto.StyleProfileResponse;
import com.stylemind.user.service.UserProfileService;
import com.stylemind.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.stylemind.common.security.UserPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final com.stylemind.user.service.AdministrativeDataService administrativeDataService;

    @GetMapping("/administrative/provinces")
    public ResponseEntity<ApiResponse<List<com.stylemind.user.dto.AdministrativeProvinceResponse>>> getProvinces() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tỉnh/thành phố thành công",
                administrativeDataService.getProvinces()));
    }

    @GetMapping("/administrative/provinces/{provinceCode}/wards")
    public ResponseEntity<ApiResponse<List<com.stylemind.user.dto.AdministrativeWardResponse>>> getWards(
            @PathVariable String provinceCode) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phường/xã thành công",
                administrativeDataService.getWards(provinceCode)));
    }

    @GetMapping("/style-profile")
    public ResponseEntity<ApiResponse<StyleProfileResponse>> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        StyleProfileResponse profile = userProfileService.getStyleProfile(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Lấy hồ sơ phong cách thành công", profile));
    }

    @PutMapping("/style-profile")
    public ResponseEntity<ApiResponse<StyleProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StyleProfileRequest request) {
        StyleProfileResponse profile = userProfileService.updateStyleProfile(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ phong cách thành công", profile));
    }

    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<List<DeliveryAddressResponse>>> getAddresses(@AuthenticationPrincipal UserPrincipal principal) {
        List<DeliveryAddressResponse> addresses = userProfileService.getAddresses(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách địa chỉ thành công", addresses));
    }

    @PostMapping("/addresses")
    public ResponseEntity<ApiResponse<DeliveryAddressResponse>> createAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DeliveryAddressRequest request) {
        DeliveryAddressResponse address = userProfileService.createAddress(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Thêm địa chỉ thành công", address));
    }

    @PutMapping("/addresses/{addressId}")
    public ResponseEntity<ApiResponse<DeliveryAddressResponse>> updateAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String addressId,
            @Valid @RequestBody DeliveryAddressRequest request) {
        DeliveryAddressResponse address = userProfileService.updateAddress(principal.getUserId(), addressId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật địa chỉ thành công", address));
    }

    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String addressId) {
        userProfileService.deleteAddress(principal.getUserId(), addressId);
        return ResponseEntity.ok(ApiResponse.success("Xóa địa chỉ thành công", null));
    }
}
