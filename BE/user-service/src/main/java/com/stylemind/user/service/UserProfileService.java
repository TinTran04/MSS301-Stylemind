package com.stylemind.user.service;

import com.stylemind.user.dto.DeliveryAddressRequest;
import com.stylemind.user.dto.DeliveryAddressResponse;
import com.stylemind.user.dto.StyleProfileRequest;
import com.stylemind.user.dto.StyleProfileResponse;
import com.stylemind.user.entity.CustomerStyleProfile;
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

    // Style Profile
    @Transactional(readOnly = true)
    public StyleProfileResponse getStyleProfile(String userId) {
        return profileRepository.findByUserId(userId)
                .map(this::mapToStyleProfileResponse)
                // Return an empty (un-persisted) profile response if none exists yet.
                // GET must not mutate the database.
                .orElse(StyleProfileResponse.builder().userId(userId).build());
    }

    public StyleProfileResponse updateStyleProfile(String userId, StyleProfileRequest request) {
        // orElseGet returns an unsaved transient instance — single save at the end
        CustomerStyleProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> CustomerStyleProfile.builder().userId(userId).build());

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

    private CustomerStyleProfile buildEmptyProfile(String userId) {
        return CustomerStyleProfile.builder().userId(userId).build();
    }

    // Delivery Addresses
    @Transactional(readOnly = true)
    public List<DeliveryAddressResponse> getAddresses(String userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    public DeliveryAddressResponse createAddress(String userId, DeliveryAddressRequest request) {
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            // Single atomic UPDATE — no race condition
            addressRepository.clearAllDefaultsByUserId(userId);
        }

        DeliveryAddress address = DeliveryAddress.builder()
                .id(StringUtil.generateUniqueId())
                .userId(userId)
                .recipientName(request.getRecipientName())
                .phoneNumber(request.getPhoneNumber())
                .addressLine(request.getAddressLine())
                .city(request.getCity())
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

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            // Single atomic UPDATE — no race condition
            addressRepository.clearAllDefaultsByUserId(userId);
        }

        address.setRecipientName(request.getRecipientName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setAddressLine(request.getAddressLine());
        address.setCity(request.getCity());
        address.setIsDefault(request.getIsDefault());

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
        return mapToAddressResponse(address);
    }

    private StyleProfileResponse mapToStyleProfileResponse(CustomerStyleProfile profile) {
        return StyleProfileResponse.builder()
                .userId(profile.getUserId())
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

    private DeliveryAddressResponse mapToAddressResponse(DeliveryAddress address) {
        return DeliveryAddressResponse.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName())
                .phoneNumber(address.getPhoneNumber())
                .addressLine(address.getAddressLine())
                .city(address.getCity())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .updatedAt(address.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .build();
    }
}