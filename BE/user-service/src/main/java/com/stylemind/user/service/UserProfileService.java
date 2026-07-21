package com.stylemind.user.service;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import com.stylemind.user.dto.DeliveryAddressRequest;
import com.stylemind.user.dto.DeliveryAddressResponse;
import com.stylemind.user.dto.UserProfileResponse;
import com.stylemind.user.entity.AddressValidationStatus;
import com.stylemind.user.entity.DeliveryAddress;
import com.stylemind.user.repository.DeliveryAddressRepository;
import com.stylemind.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final DeliveryAddressRepository addressRepository;
    private final AdministrativeDataService administrativeDataService;
    private final VietnamesePhoneNumberService phoneNumberService;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String userId) {
        return userProfileRepository.findById(userId)
                .map(profile -> UserProfileResponse.builder()
                        .userId(profile.getUserId())
                        .displayName(profile.getDisplayName())
                        .build())
                .orElseGet(() -> UserProfileResponse.builder().userId(userId).build());
    }

    @Transactional(readOnly = true)
    public List<DeliveryAddressResponse> getAddresses(String userId) {
        return addressRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing((DeliveryAddress address) -> Boolean.TRUE.equals(address.getIsDefault())).reversed()
                        .thenComparing(DeliveryAddress::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::mapToAddressResponse)
                .toList();
    }

    public DeliveryAddressResponse createAddress(String userId, DeliveryAddressRequest request) {
        AdministrativeDataService.AddressAdministrativeSnapshot administrative =
                administrativeDataService.validateAndResolve(request.getProvinceCode(), request.getWardCode());
        String normalizedPhone = phoneNumberService.normalize(request.getPhoneNumber());
        addressRepository.lockDefaultAddressSlot(userId);
        boolean shouldBeDefault = Boolean.TRUE.equals(request.getIsDefault())
                || addressRepository.findByUserId(userId).isEmpty();
        if (shouldBeDefault) {
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
                .isDefault(shouldBeDefault)
                .build();

        return mapToAddressResponse(addressRepository.save(address));
    }

    public DeliveryAddressResponse updateAddress(String userId, String addressId, DeliveryAddressRequest request) {
        DeliveryAddress address = requireOwnedAddress(userId, addressId);
        addressRepository.lockDefaultAddressSlot(userId);
        AdministrativeDataService.AddressAdministrativeSnapshot administrative =
                administrativeDataService.validateAndResolve(request.getProvinceCode(), request.getWardCode());
        String normalizedPhone = phoneNumberService.normalize(request.getPhoneNumber());

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.clearAllDefaultsByUserId(userId);
            address.setIsDefault(true);
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

        return mapToAddressResponse(addressRepository.save(address));
    }

    public DeliveryAddressResponse setDefaultAddress(String userId, String addressId) {
        DeliveryAddress address = requireOwnedAddress(userId, addressId);
        addressRepository.lockDefaultAddressSlot(userId);
        if (address.getValidationStatus() != AddressValidationStatus.VALID) {
            throw new BusinessException("SHIPPING_ADDRESS_NOT_VALIDATED",
                    "Địa chỉ cần được cập nhật trước khi chọn giao hàng", 409);
        }

        addressRepository.clearAllDefaultsByUserId(userId);
        address.setIsDefault(true);
        return mapToAddressResponse(addressRepository.save(address));
    }

    public void deleteAddress(String userId, String addressId) {
        DeliveryAddress address = requireOwnedAddress(userId, addressId);
        addressRepository.lockDefaultAddressSlot(userId);
        DeliveryAddress promotedAddress = Boolean.TRUE.equals(address.getIsDefault())
                ? addressRepository.findByUserId(userId).stream()
                        .filter(candidate -> !addressId.equals(candidate.getId()))
                        .filter(candidate -> candidate.getValidationStatus() == AddressValidationStatus.VALID)
                        .min(Comparator.comparing(DeliveryAddress::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(DeliveryAddress::getId))
                        .orElse(null)
                : null;

        addressRepository.delete(address);
        if (promotedAddress != null) {
            addressRepository.clearAllDefaultsByUserId(userId);
            promotedAddress.setIsDefault(true);
            addressRepository.save(promotedAddress);
        }
    }

    /**
     * Internal use only. The caller provides the authenticated order owner;
     * the returned snapshot is authoritative and safe for Order Service to persist.
     */
    @Transactional(readOnly = true)
    public DeliveryAddressResponse getAddressById(String userId, String addressId) {
        DeliveryAddress address = requireOwnedAddress(userId, addressId);
        if (address.getValidationStatus() != AddressValidationStatus.VALID) {
            throw new BusinessException("SHIPPING_ADDRESS_NOT_VALIDATED",
                    "Địa chỉ cần được cập nhật trước khi thanh toán", 409);
        }
        return mapToAddressResponse(address);
    }

    private DeliveryAddress requireOwnedAddress(String userId, String addressId) {
        DeliveryAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException("ADDRESS_NOT_FOUND", "Không tìm thấy địa chỉ", 404));
        if (!userId.equals(address.getUserId())) {
            throw new BusinessException("ACCESS_DENIED", "Không có quyền truy cập địa chỉ này", 403);
        }
        return address;
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
                .isDefault(Boolean.TRUE.equals(address.getIsDefault()))
                .createdAt(address.getCreatedAt() == null ? null
                        : address.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant())
                .updatedAt(address.getUpdatedAt() == null ? null
                        : address.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant())
                .build();
    }
}
