package com.stylemind.user.service;

import com.stylemind.user.dto.DeliveryAddressRequest;
import com.stylemind.user.dto.DeliveryAddressResponse;
import com.stylemind.user.dto.StyleProfileRequest;
import com.stylemind.user.dto.StyleProfileResponse;
import com.stylemind.user.entity.CustomerStyleProfile;
import com.stylemind.user.entity.AddressValidationStatus;
import com.stylemind.user.entity.DeliveryAddress;
import com.stylemind.user.repository.CustomerStyleProfileRepository;
import com.stylemind.user.repository.DeliveryAddressRepository;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserProfileService {

    private final CustomerStyleProfileRepository profileRepository;
    private final DeliveryAddressRepository addressRepository;
    private final AdministrativeDataService administrativeDataService;
    private final VietnamesePhoneNumberService phoneNumberService;

    // Style Profile
    @Transactional
    public StyleProfileResponse getStyleProfile(String userId) {
        return mapToStyleProfileResponse(ensureProfile(userId));
    }

    @Transactional
    public StyleProfileResponse updateStyleProfile(String userId, StyleProfileRequest request) {
        CustomerStyleProfile profile = ensureProfile(userId);

        profile.setDisplayName(request.getDisplayName());
        profile.setGender(request.getGender());
        profile.setAge(request.getAge());
        profile.setHeightCm(request.getHeightCm());
        profile.setWeightKg(request.getWeightKg());
        profile.setBodyMorphology(request.getBodyMorphology());
        profile.setPreferredFit(request.getPreferredFit());
        profile.setStylePersonas(request.getStylePersonas());

        profile = profileRepository.save(profile);
        return mapToStyleProfileResponse(profile);
    }

    // Delivery Addresses
    @Transactional
    public List<DeliveryAddressResponse> getAddresses(String userId) {
        ensureProfile(userId);
        return addressRepository.findByUserId(userId).stream()
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DeliveryAddressResponse createAddress(String userId, DeliveryAddressRequest request) {
        AdministrativeDataService.AddressAdministrativeSnapshot administrative =
                administrativeDataService.validateAndResolve(request.getProvinceCode(), request.getWardCode());
        String normalizedPhone = phoneNumberService.normalize(request.getPhoneNumber());
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            // Single atomic UPDATE — no race condition
            addressRepository.clearAllDefaultsByUserId(userId);
        }

        DeliveryAddress address = DeliveryAddress.builder()
                .id(StringUtil.generateUniqueId())
                .userId(userId)
                .recipientName(request.getRecipientName())
                .phoneNumber(normalizedPhone)
                .addressLine(request.getAddressLine())
                .city(administrative.provinceName())
                .provinceCode(administrative.provinceCode())
                .provinceName(administrative.provinceName())
                .wardCode(administrative.wardCode())
                .wardName(administrative.wardName())
                .shippingNote(request.getShippingNote())
                .validationStatus(AddressValidationStatus.VALID)
                .administrativeDataVersion(administrative.dataVersion())
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .build();

        return mapToAddressResponse(addressRepository.save(address));
    }

    public DeliveryAddressResponse updateAddress(String userId, String addressId, DeliveryAddressRequest request) {
        DeliveryAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException("ADDRESS_NOT_FOUND", "Không tìm thấy địa chỉ", 404));

        if (!address.getUserId().equals(userId)) {
            throw new BusinessException("ACCESS_DENIED", "Không có quyền truy cập địa chỉ này", 403);
        }

        AdministrativeDataService.AddressAdministrativeSnapshot administrative =
                administrativeDataService.validateAndResolve(request.getProvinceCode(), request.getWardCode());
        String normalizedPhone = phoneNumberService.normalize(request.getPhoneNumber());

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            // Single atomic UPDATE — no race condition
            addressRepository.clearAllDefaultsByUserId(userId);
        }

        address.setRecipientName(request.getRecipientName());
        address.setPhoneNumber(normalizedPhone);
        address.setAddressLine(request.getAddressLine());
        address.setCity(administrative.provinceName());
        address.setProvinceCode(administrative.provinceCode());
        address.setProvinceName(administrative.provinceName());
        address.setWardCode(administrative.wardCode());
        address.setWardName(administrative.wardName());
        address.setShippingNote(request.getShippingNote());
        address.setValidationStatus(AddressValidationStatus.VALID);
        address.setAdministrativeDataVersion(administrative.dataVersion());
        address.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));

        return mapToAddressResponse(addressRepository.save(address));
    }

    public void deleteAddress(String userId, String addressId) {
        DeliveryAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException("ADDRESS_NOT_FOUND", "Không tìm thấy địa chỉ", 404));

        if (!address.getUserId().equals(userId)) {
            throw new BusinessException("ACCESS_DENIED", "Không có quyền xóa địa chỉ này", 403);
        }

        addressRepository.delete(address);
    }

    /**
     * Internal use only — called by InternalUserController for cross-service lookups.
     * Validates that the address belongs to the given userId before returning.
     */
    @Transactional(readOnly = true)
    public DeliveryAddressResponse getAddressById(String userId, String addressId) {
        DeliveryAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException("ADDRESS_NOT_FOUND", "Không tìm thấy địa chỉ", 404));
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException("ACCESS_DENIED", "Địa chỉ không thuộc người dùng này", 403);
        }
        if (address.getValidationStatus() != AddressValidationStatus.VALID) {
            throw new BusinessException("SHIPPING_ADDRESS_NOT_VALIDATED", "Địa chỉ cần được cập nhật trước khi thanh toán", 409);
        }
        return mapToAddressResponse(address);
    }

    private StyleProfileResponse mapToStyleProfileResponse(CustomerStyleProfile profile) {
        return StyleProfileResponse.builder()
                .userId(profile.getUserId())
                .displayName(profile.getDisplayName())
                .gender(profile.getGender())
                .age(profile.getAge())
                .heightCm(profile.getHeightCm())
                .weightKg(profile.getWeightKg())
                .bodyMorphology(profile.getBodyMorphology())
                .preferredFit(profile.getPreferredFit())
                .stylePersonas(profile.getStylePersonas())
                .createdAt(profile.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .updatedAt(profile.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .build();
    }

    private CustomerStyleProfile ensureProfile(String userId) {
        profileRepository.insertProfileShell(userId);
        return profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Profile shell was not available after initialization for user " + userId));
    }

    private DeliveryAddressResponse mapToAddressResponse(DeliveryAddress address) {
        return DeliveryAddressResponse.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName())
                .phoneNumber(address.getPhoneNumber())
                .addressLine(address.getAddressLine())
                .city(address.getCity())
                .userId(address.getUserId())
                .provinceCode(address.getProvinceCode())
                .provinceName(address.getProvinceName())
                .wardCode(address.getWardCode())
                .wardName(address.getWardName())
                .shippingNote(address.getShippingNote())
                .validationStatus(address.getValidationStatus())
                .administrativeDataVersion(address.getAdministrativeDataVersion())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .updatedAt(address.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .build();
    }
}
