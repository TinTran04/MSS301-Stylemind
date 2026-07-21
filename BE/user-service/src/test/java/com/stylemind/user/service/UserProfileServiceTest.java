package com.stylemind.user.service;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.user.dto.DeliveryAddressRequest;
import com.stylemind.user.dto.DeliveryAddressResponse;
import com.stylemind.user.entity.AddressValidationStatus;
import com.stylemind.user.entity.DeliveryAddress;
import com.stylemind.user.repository.DeliveryAddressRepository;
import com.stylemind.user.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private DeliveryAddressRepository addressRepository;

    @Mock
    private AdministrativeDataService administrativeDataService;

    @Mock
    private VietnamesePhoneNumberService phoneNumberService;

    @InjectMocks
    private UserProfileService userProfileService;

    @org.junit.jupiter.api.BeforeEach
    void stubAddressValidation() {
        lenient().when(administrativeDataService.validateAndResolve(anyString(), anyString()))
                .thenReturn(new AdministrativeDataService.AddressAdministrativeSnapshot(
                        "01", "Thành phố Hà Nội", "00004", "Phường Ba Đình", "v4.0.0"));
        lenient().when(phoneNumberService.normalize(anyString())).thenReturn("+84901234567");
    }

    // ─── Delivery Addresses ───────────────────────────────────────────────────

    @Test
    void getAddresses_returnsOnlyAddressesWithoutCreatingLegacyProfileData() {
        when(addressRepository.findByUserId("user-1")).thenReturn(List.of());

        List<DeliveryAddressResponse> result = userProfileService.getAddresses("user-1");

        assertThat(result).isEmpty();
        verifyNoInteractions(userProfileRepository);
    }

    @Test
    void getAddresses_toleratesLegacyNullDefaultFlags() {
        DeliveryAddress legacyAddress = buildAddress("legacy-address", "user-1", false);
        legacyAddress.setIsDefault(null);
        when(addressRepository.findByUserId("user-1")).thenReturn(List.of(legacyAddress));

        List<DeliveryAddressResponse> result = userProfileService.getAddresses("user-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsDefault()).isFalse();
    }

    @Test
    void createAddress_withIsDefaultTrue_clearsOldDefaultAtomically() {
        when(addressRepository.save(any())).thenAnswer(inv -> {
            DeliveryAddress a = inv.getArgument(0);
            a.setCreatedAt(LocalDateTime.now());
            a.setUpdatedAt(LocalDateTime.now());
            return a;
        });

        DeliveryAddressRequest request = buildAddressRequest(true);
        userProfileService.createAddress("user-1", request);

        // Must use atomic UPDATE, not read-then-write
        verify(addressRepository, times(1)).clearAllDefaultsByUserId("user-1");
        verify(addressRepository, never()).findByUserIdAndIsDefaultTrue(any());
    }

    @Test
    void createAddress_withIsDefaultFalse_doesNotClearDefaults() {
        when(addressRepository.findByUserId("user-1"))
                .thenReturn(List.of(buildAddress("existing-address", "user-1", true)));
        when(addressRepository.save(any())).thenAnswer(inv -> {
            DeliveryAddress a = inv.getArgument(0);
            a.setCreatedAt(LocalDateTime.now());
            a.setUpdatedAt(LocalDateTime.now());
            return a;
        });

        DeliveryAddressRequest request = buildAddressRequest(false);
        userProfileService.createAddress("user-1", request);

        verify(addressRepository, never()).clearAllDefaultsByUserId(any());
    }

    @Test
    void createAddress_firstAddressBecomesDefaultEvenWhenRequestDoesNotSelectIt() {
        when(addressRepository.findByUserId("user-1")).thenReturn(List.of());
        when(addressRepository.save(any())).thenAnswer(inv -> {
            DeliveryAddress address = inv.getArgument(0);
            address.setCreatedAt(LocalDateTime.now());
            address.setUpdatedAt(LocalDateTime.now());
            return address;
        });

        userProfileService.createAddress("user-1", buildAddressRequest(false));

        verify(addressRepository).lockDefaultAddressSlot("user-1");
        verify(addressRepository).save(argThat(DeliveryAddress::getIsDefault));
    }

    @Test
    void updateAddress_toDefault_clearsOldDefaultAtomically() {
        DeliveryAddress existing = buildAddress("addr-1", "user-1", false);
        when(addressRepository.findById("addr-1")).thenReturn(Optional.of(existing));
        when(addressRepository.save(any())).thenAnswer(inv -> {
            DeliveryAddress a = inv.getArgument(0);
            a.setUpdatedAt(LocalDateTime.now());
            return a;
        });

        DeliveryAddressRequest request = buildAddressRequest(true);
        userProfileService.updateAddress("user-1", "addr-1", request);

        verify(addressRepository, times(1)).clearAllDefaultsByUserId("user-1");
    }

    @Test
    void setDefaultAddressClearsPreviousDefaultAndReturnsPersistedAddress() {
        DeliveryAddress address = buildAddress("addr-1", "user-1", false);
        when(addressRepository.findById("addr-1")).thenReturn(Optional.of(address));
        when(addressRepository.save(address)).thenReturn(address);

        DeliveryAddressResponse result = userProfileService.setDefaultAddress("user-1", "addr-1");

        assertThat(result.getIsDefault()).isTrue();
        verify(addressRepository).clearAllDefaultsByUserId("user-1");
    }

    @Test
    void updateAddress_otherUser_throws403() {
        DeliveryAddress existing = buildAddress("addr-1", "other-user", false);
        when(addressRepository.findById("addr-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
                userProfileService.updateAddress("user-1", "addr-1", buildAddressRequest(false)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không có quyền");
    }

    @Test
    void deleteAddress_otherUser_throws403() {
        DeliveryAddress existing = buildAddress("addr-1", "other-user", false);
        when(addressRepository.findById("addr-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userProfileService.deleteAddress("user-1", "addr-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không có quyền");
    }

    @Test
    void deleteAddress_notFound_throws404() {
        when(addressRepository.findById("ghost-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.deleteAddress("user-1", "ghost-id"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không tìm thấy");
    }

    @Test
    void deleteDefaultAddressPromotesOldestRemainingAddress() {
        DeliveryAddress defaultAddress = buildAddress("addr-default", "user-1", true);
        defaultAddress.setCreatedAt(LocalDateTime.of(2026, 7, 1, 10, 0));
        DeliveryAddress promotedAddress = buildAddress("addr-next", "user-1", false);
        promotedAddress.setCreatedAt(LocalDateTime.of(2026, 7, 2, 10, 0));
        when(addressRepository.findById("addr-default")).thenReturn(Optional.of(defaultAddress));
        when(addressRepository.findByUserId("user-1")).thenReturn(List.of(defaultAddress, promotedAddress));

        userProfileService.deleteAddress("user-1", "addr-default");

        assertThat(promotedAddress.getIsDefault()).isTrue();
        verify(addressRepository).delete(defaultAddress);
    }

    @Test
    void getAddressById_wrongUser_throws403() {
        DeliveryAddress addr = buildAddress("addr-1", "other-user", false);
        when(addressRepository.findById("addr-1")).thenReturn(Optional.of(addr));

        assertThatThrownBy(() -> userProfileService.getAddressById("user-1", "addr-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không có quyền");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private DeliveryAddress buildAddress(String id, String userId, boolean isDefault) {
        DeliveryAddress a = new DeliveryAddress();
        a.setId(id);
        a.setUserId(userId);
        a.setRecipientName("Test User");
        a.setPhoneNumber("0901234567");
        a.setAddressLine("123 Test St");
        a.setCity("Hanoi");
        a.setProvinceCode("01");
        a.setProvinceName("Thành phố Hà Nội");
        a.setWardCode("00004");
        a.setWardName("Phường Ba Đình");
        a.setValidationStatus(AddressValidationStatus.VALID);
        a.setIsDefault(isDefault);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        return a;
    }

    private DeliveryAddressRequest buildAddressRequest(boolean isDefault) {
        return DeliveryAddressRequest.builder()
                .recipientName("Test User")
                .phoneNumber("0901234567")
                .addressLine("123 Test St")
                .provinceCode("01")
                .wardCode("00004")
                .isDefault(isDefault)
                .build();
    }
}
