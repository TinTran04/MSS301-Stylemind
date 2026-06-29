package com.stylemind.user.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.security.UserPrincipal;
import com.stylemind.user.dto.AddressPatchRequest;
import com.stylemind.user.dto.AddressRequest;
import com.stylemind.user.dto.AddressResponse;
import com.stylemind.user.dto.DeliveryAddressRequest;
import com.stylemind.user.dto.DeliveryAddressResponse;
import com.stylemind.user.dto.StyleProfileRequest;
import com.stylemind.user.dto.StyleProfileResponse;
import com.stylemind.user.dto.UserProfileRequest;
import com.stylemind.user.dto.UserProfileResponse;
import com.stylemind.user.service.UserProfileService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(name = "X-User-Id", required = false) String internalUserId) {
        UserProfileResponse profile = userProfileService.getProfile(resolveUserId(principal, internalUserId));
        return ResponseEntity.ok(ApiResponse.success("Get profile successfully", profile));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(name = "X-User-Id", required = false) String internalUserId,
            @Valid @RequestBody UserProfileRequest request) {
        UserProfileResponse profile = userProfileService.updateProfile(resolveUserId(principal, internalUserId), request);
        return ResponseEntity.ok(ApiResponse.success("Update profile successfully", profile));
    }

    @GetMapping("/me/addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(name = "X-User-Id", required = false) String internalUserId) {
        List<AddressResponse> addresses = userProfileService.getMyAddresses(resolveUserId(principal, internalUserId));
        return ResponseEntity.ok(ApiResponse.success("Get addresses successfully", addresses));
    }

    @PostMapping("/me/addresses")
    public ResponseEntity<ApiResponse<AddressResponse>> createMyAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(name = "X-User-Id", required = false) String internalUserId,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse address = userProfileService.createMyAddress(resolveUserId(principal, internalUserId), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Create address successfully", address));
    }

    @PatchMapping("/me/addresses/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateMyAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(name = "X-User-Id", required = false) String internalUserId,
            @PathVariable String addressId,
            @Valid @RequestBody AddressPatchRequest request) {
        AddressResponse address = userProfileService.updateMyAddress(
                resolveUserId(principal, internalUserId),
                addressId,
                request);
        return ResponseEntity.ok(ApiResponse.success("Update address successfully", address));
    }

    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> deleteMyAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(name = "X-User-Id", required = false) String internalUserId,
            @PathVariable String addressId) {
        userProfileService.deleteMyAddress(resolveUserId(principal, internalUserId), addressId);
        return ResponseEntity.ok(ApiResponse.success("Delete address successfully", Map.of("deleted", true)));
    }

    @PutMapping("/me/addresses/{addressId}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setMyDefaultAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(name = "X-User-Id", required = false) String internalUserId,
            @PathVariable String addressId) {
        AddressResponse address = userProfileService.setMyDefaultAddress(resolveUserId(principal, internalUserId), addressId);
        return ResponseEntity.ok(ApiResponse.success("Set default address successfully", address));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<StyleProfileResponse>> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        StyleProfileResponse profile = userProfileService.getStyleProfile(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Lấy hồ sơ phong cách thành công", profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<StyleProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StyleProfileRequest request) {
        StyleProfileResponse profile = userProfileService.updateStyleProfile(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ phong cách thành công", profile));
    }

    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<List<DeliveryAddressResponse>>> getAddresses(
            @AuthenticationPrincipal UserPrincipal principal) {
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

    private String resolveUserId(UserPrincipal principal, String internalUserId) {
        if (principal != null && principal.getUserId() != null && !principal.getUserId().isBlank()) {
            return principal.getUserId();
        }
        if (internalUserId != null && !internalUserId.isBlank()) {
            return internalUserId;
        }
        throw new BusinessException("UNAUTHENTICATED", "Authenticated user context is required", 401);
    }
}
