package com.stylemind.user.service;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import com.stylemind.user.dto.AddressPatchRequest;
import com.stylemind.user.dto.AddressRequest;
import com.stylemind.user.dto.AddressResponse;
import com.stylemind.user.dto.AdminUserDetailResponse;
import com.stylemind.user.dto.AdminUserPageResponse;
import com.stylemind.user.dto.DeliveryAddressRequest;
import com.stylemind.user.dto.DeliveryAddressResponse;
import com.stylemind.user.dto.StyleProfileRequest;
import com.stylemind.user.dto.StyleProfileResponse;
import com.stylemind.user.dto.UserProfileRequest;
import com.stylemind.user.dto.UserProfileResponse;
import com.stylemind.user.entity.Address;
import com.stylemind.user.entity.CustomerStyleProfile;
import com.stylemind.user.entity.DeliveryAddress;
import com.stylemind.user.entity.UserProfile;
import com.stylemind.user.repository.AddressRepository;
import com.stylemind.user.repository.CustomerStyleProfileRepository;
import com.stylemind.user.repository.DeliveryAddressRepository;
import com.stylemind.user.repository.UserProfileRepository;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserProfileService {

    private final CustomerStyleProfileRepository profileRepository;
    private final DeliveryAddressRepository addressRepository;
    private final UserProfileRepository userProfileRepository;
    private final AddressRepository shippingAddressRepository;

    public UserProfileResponse getProfile(String userId) {
        UserProfile profile = userProfileRepository.findById(parseUserId(userId))
                .orElseThrow(() -> new BusinessException("PROFILE_NOT_FOUND", "Profile not found", 404));
        return mapToUserProfileResponse(profile);
    }

    public UserProfileResponse updateProfile(String userId, UserProfileRequest request) {
        UserProfile profile = userProfileRepository.findById(parseUserId(userId))
                .orElseThrow(() -> new BusinessException("PROFILE_NOT_FOUND", "Profile not found", 404));

        profile.setFullName(trimToNull(request.getFullName()));
        profile.setPhone(trimToNull(request.getPhone()));
        profile.setAvatarUrl(trimToNull(request.getAvatarUrl()));
        profile.setGender(trimToNull(request.getGender()));
        profile.setDateOfBirth(request.getDateOfBirth());

        return mapToUserProfileResponse(userProfileRepository.save(profile));
    }

    public List<AddressResponse> getMyAddresses(String userId) {
        UUID currentUserId = parseUserId(userId);
        return shippingAddressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtAsc(currentUserId).stream()
                .map(this::mapToAddressBookResponse)
                .collect(Collectors.toList());
    }

    public AddressResponse createMyAddress(String userId, AddressRequest request) {
        UUID currentUserId = parseUserId(userId);
        List<Address> lockedAddresses = shippingAddressRepository.findByUserIdForUpdate(currentUserId);
        boolean shouldBeDefault = lockedAddresses.isEmpty() || Boolean.TRUE.equals(request.getIsDefault());

        if (shouldBeDefault) {
            lockedAddresses.forEach(address -> address.setDefaultAddress(false));
            shippingAddressRepository.flush();
        }

        Address address = Address.builder()
                .id(UUID.randomUUID())
                .userId(currentUserId)
                .receiverName(trimRequired(request.getReceiverName()))
                .receiverPhone(trimRequired(request.getReceiverPhone()))
                .province(trimRequired(request.getProvince()))
                .district(trimRequired(request.getDistrict()))
                .ward(trimRequired(request.getWard()))
                .streetAddress(trimRequired(request.getStreetAddress()))
                .defaultAddress(shouldBeDefault)
                .build();

        return mapToAddressBookResponse(shippingAddressRepository.save(address));
    }

    public AddressResponse updateMyAddress(String userId, String addressId, AddressPatchRequest request) {
        UUID currentUserId = parseUserId(userId);
        UUID currentAddressId = parseAddressId(addressId);
        Address address = shippingAddressRepository.findByIdAndUserIdForUpdate(currentAddressId, currentUserId)
                .orElseThrow(() -> addressNotFound());

        if (request.getReceiverName() != null) {
            address.setReceiverName(trimRequired(request.getReceiverName()));
        }
        if (request.getReceiverPhone() != null) {
            address.setReceiverPhone(trimRequired(request.getReceiverPhone()));
        }
        if (request.getProvince() != null) {
            address.setProvince(trimRequired(request.getProvince()));
        }
        if (request.getDistrict() != null) {
            address.setDistrict(trimRequired(request.getDistrict()));
        }
        if (request.getWard() != null) {
            address.setWard(trimRequired(request.getWard()));
        }
        if (request.getStreetAddress() != null) {
            address.setStreetAddress(trimRequired(request.getStreetAddress()));
        }
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            setDefaultAddress(currentUserId, address);
        }

        return mapToAddressBookResponse(shippingAddressRepository.save(address));
    }

    public void deleteMyAddress(String userId, String addressId) {
        UUID currentUserId = parseUserId(userId);
        UUID currentAddressId = parseAddressId(addressId);
        Address address = shippingAddressRepository.findByIdAndUserIdForUpdate(currentAddressId, currentUserId)
                .orElseThrow(() -> addressNotFound());
        boolean wasDefault = address.isDefaultAddress();

        shippingAddressRepository.delete(address);
        shippingAddressRepository.flush();

        if (wasDefault) {
            shippingAddressRepository.findByUserIdForUpdate(currentUserId).stream()
                    .min(Comparator.comparing(Address::getCreatedAt))
                    .ifPresent(nextDefault -> nextDefault.setDefaultAddress(true));
        }
    }

    public AddressResponse setMyDefaultAddress(String userId, String addressId) {
        UUID currentUserId = parseUserId(userId);
        UUID currentAddressId = parseAddressId(addressId);
        List<Address> lockedAddresses = shippingAddressRepository.findByUserIdForUpdate(currentUserId);
        Address address = lockedAddresses.stream()
                .filter(candidate -> candidate.getId().equals(currentAddressId))
                .findFirst()
                .orElseThrow(() -> addressNotFound());
        setDefaultAddress(lockedAddresses, address);
        return mapToAddressBookResponse(shippingAddressRepository.save(address));
    }

    @Transactional(readOnly = true)
    public AdminUserPageResponse listUsersForAdmin(int page, int size, String search) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<UserProfile> profiles = trimToNull(search) == null
                ? userProfileRepository.findAll(pageRequest)
                : userProfileRepository.findByFullNameContainingIgnoreCase(search.trim(), pageRequest);

        return AdminUserPageResponse.builder()
                .items(profiles.getContent().stream()
                        .map(this::mapToUserProfileResponse)
                        .toList())
                .page(profiles.getNumber())
                .size(profiles.getSize())
                .totalItems(profiles.getTotalElements())
                .totalPages(profiles.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserForAdmin(String userId) {
        UUID currentUserId = parseUserId(userId);
        UserProfile profile = userProfileRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException("PROFILE_NOT_FOUND", "Profile not found", 404));

        return AdminUserDetailResponse.builder()
                .profile(mapToUserProfileResponse(profile))
                .addresses(shippingAddressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtAsc(currentUserId).stream()
                        .map(this::mapToAddressBookResponse)
                        .toList())
                .build();
    }

    // Style Profile
    public StyleProfileResponse getStyleProfile(String userId) {
        CustomerStyleProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId));
        return mapToStyleProfileResponse(profile);
    }

    public StyleProfileResponse updateStyleProfile(String userId, StyleProfileRequest request) {
        CustomerStyleProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId));

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

    private CustomerStyleProfile createDefaultProfile(String userId) {
        return profileRepository.save(CustomerStyleProfile.builder()
                .userId(userId)
                .build());
    }

    // Delivery Addresses
    public List<DeliveryAddressResponse> getAddresses(String userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    public DeliveryAddressResponse createAddress(String userId, DeliveryAddressRequest request) {
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.findByUserIdAndIsDefaultTrue(userId)
                    .ifPresent(addr -> {
                        addr.setIsDefault(false);
                        addressRepository.save(addr);
                    });
        }

        DeliveryAddress address = DeliveryAddress.builder()
                .id(StringUtil.generateUniqueId())
                .userId(userId)
                .recipientName(request.getRecipientName())
                .phoneNumber(request.getPhoneNumber())
                .addressLine(request.getAddressLine())
                .city(request.getCity())
                .isDefault(request.getIsDefault())
                .build();

        return mapToAddressResponse(addressRepository.save(address));
    }

    public DeliveryAddressResponse updateAddress(String userId, String addressId, DeliveryAddressRequest request) {
        DeliveryAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new BusinessException("ADDRESS_NOT_FOUND", "Không tìm thấy địa chỉ", 404));

        if (!address.getUserId().equals(userId)) {
            throw new BusinessException("ACCESS_DENIED", "Không có quyền truy cập địa chỉ này", 403);
        }

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.findByUserIdAndIsDefaultTrue(userId)
                    .ifPresent(addr -> {
                        addr.setIsDefault(false);
                        addressRepository.save(addr);
                    });
        }

        address.setRecipientName(request.getRecipientName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setAddressLine(request.getAddressLine());
        address.setCity(request.getCity());
        address.setIsDefault(request.getIsDefault());

        return mapToAddressResponse(addressRepository.save(address));
    }

    public void deleteAddress(String userId, String addressId) {
        DeliveryAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new BusinessException("ADDRESS_NOT_FOUND", "Không tìm thấy địa chỉ", 404));

        if (!address.getUserId().equals(userId)) {
            throw new BusinessException("ACCESS_DENIED", "Không có quyền xóa địa chỉ này", 403);
        }

        addressRepository.delete(address);
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
                .userId(address.getUserId())
                .recipientName(address.getRecipientName())
                .phoneNumber(address.getPhoneNumber())
                .addressLine(address.getAddressLine())
                .city(address.getCity())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .build();
    }

    private UserProfileResponse mapToUserProfileResponse(UserProfile profile) {
        return UserProfileResponse.builder()
                .userId(profile.getUserId().toString())
                .fullName(profile.getFullName())
                .phone(profile.getPhone())
                .avatarUrl(profile.getAvatarUrl())
                .gender(profile.getGender())
                .dateOfBirth(profile.getDateOfBirth())
                .createdAt(profile.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant())
                .updatedAt(profile.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant())
                .build();
    }

    private AddressResponse mapToAddressBookResponse(Address address) {
        return AddressResponse.builder()
                .addressId(address.getId().toString())
                .receiverName(address.getReceiverName())
                .receiverPhone(address.getReceiverPhone())
                .province(address.getProvince())
                .district(address.getDistrict())
                .ward(address.getWard())
                .streetAddress(address.getStreetAddress())
                .isDefault(address.isDefaultAddress())
                .createdAt(address.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant())
                .updatedAt(address.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant())
                .build();
    }

    private void setDefaultAddress(UUID userId, Address newDefaultAddress) {
        setDefaultAddress(shippingAddressRepository.findByUserIdForUpdate(userId), newDefaultAddress);
    }

    private void setDefaultAddress(List<Address> lockedAddresses, Address newDefaultAddress) {
        lockedAddresses.forEach(address -> address.setDefaultAddress(false));
        shippingAddressRepository.flush();
        newDefaultAddress.setDefaultAddress(true);
    }

    private UUID parseUserId(String userId) {
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("INVALID_USER_CONTEXT", "Invalid authenticated user context", 401);
        }
    }

    private UUID parseAddressId(String addressId) {
        try {
            return UUID.fromString(addressId);
        } catch (IllegalArgumentException ex) {
            throw addressNotFound();
        }
    }

    private BusinessException addressNotFound() {
        return new BusinessException("ADDRESS_NOT_FOUND", "Address not found", 404);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimRequired(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "Address field must not be blank", 400);
        }
        return trimmed;
    }
}
