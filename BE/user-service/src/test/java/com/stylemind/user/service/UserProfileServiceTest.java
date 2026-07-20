package com.stylemind.user.service;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import com.stylemind.user.dto.DeliveryAddressRequest;
import com.stylemind.user.dto.DeliveryAddressResponse;
import com.stylemind.user.dto.StyleProfileRequest;
import com.stylemind.user.dto.StyleProfileResponse;
import com.stylemind.user.entity.CustomerStyleProfile;
import com.stylemind.user.entity.AddressValidationStatus;
import com.stylemind.user.entity.DeliveryAddress;
import com.stylemind.user.repository.CustomerStyleProfileRepository;
import com.stylemind.user.repository.DeliveryAddressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private CustomerStyleProfileRepository profileRepository;

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

    // ─── Style Profile ────────────────────────────────────────────────────────

    @Test
    void getStyleProfile_noProfileExists_persistsAndReturnsShell() {
        CustomerStyleProfile shell = buildProfile("user-1");
        when(profileRepository.findById("user-1")).thenReturn(Optional.of(shell));

        StyleProfileResponse result = userProfileService.getStyleProfile("user-1");

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo("user-1");
        assertThat(result.getGender()).isNull();
        verify(profileRepository).insertProfileShell("user-1");
    }

    @Test
    void getStyleProfile_profileExists_returnsMappedResponse() {
        CustomerStyleProfile profile = buildProfile("user-1");
        profile.setGender("MALE");
        profile.setAge(25);
        when(profileRepository.findById("user-1")).thenReturn(Optional.of(profile));

        StyleProfileResponse result = userProfileService.getStyleProfile("user-1");

        assertThat(result.getGender()).isEqualTo("MALE");
        assertThat(result.getAge()).isEqualTo(25);
        verify(profileRepository).insertProfileShell("user-1");
    }

    @Test
    void updateStyleProfile_newUser_createsAndSavesProfile() {
        CustomerStyleProfile shell = buildProfile("new-user");
        when(profileRepository.findById("new-user")).thenReturn(Optional.of(shell));
        when(profileRepository.save(any())).thenAnswer(inv -> {
            CustomerStyleProfile p = inv.getArgument(0);
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            return p;
        });

        StyleProfileRequest request = StyleProfileRequest.builder()
                .gender("FEMALE").age(22)
                .heightCm(BigDecimal.valueOf(165)).weightKg(BigDecimal.valueOf(55))
                .build();

        StyleProfileResponse result = userProfileService.updateStyleProfile("new-user", request);

        assertThat(result.getUserId()).isEqualTo("new-user");
        assertThat(result.getGender()).isEqualTo("FEMALE");
        verify(profileRepository, times(1)).save(any()); // single save
    }

    @Test
    void updateStyleProfile_existingUser_updatesProfile() {
        CustomerStyleProfile existing = buildProfile("user-1");
        existing.setGender("MALE");
        when(profileRepository.findById("user-1")).thenReturn(Optional.of(existing));
        when(profileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StyleProfileRequest request = StyleProfileRequest.builder()
                .gender("FEMALE").age(30).build();

        StyleProfileResponse result = userProfileService.updateStyleProfile("user-1", request);

        assertThat(result.getGender()).isEqualTo("FEMALE");
        assertThat(result.getAge()).isEqualTo(30);
        verify(profileRepository, times(1)).save(any()); // still single save
    }

    // ─── Delivery Addresses ───────────────────────────────────────────────────

    @Test
    void getAddresses_firstAccess_persistsProfileShell() {
        CustomerStyleProfile shell = buildProfile("user-1");
        when(profileRepository.findById("user-1")).thenReturn(Optional.of(shell));
        when(addressRepository.findByUserId("user-1")).thenReturn(List.of());

        List<DeliveryAddressResponse> result = userProfileService.getAddresses("user-1");

        assertThat(result).isEmpty();
        verify(profileRepository).insertProfileShell("user-1");
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
    void getAddressById_wrongUser_throws403() {
        DeliveryAddress addr = buildAddress("addr-1", "other-user", false);
        when(addressRepository.findById("addr-1")).thenReturn(Optional.of(addr));

        assertThatThrownBy(() -> userProfileService.getAddressById("user-1", "addr-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Địa chỉ không thuộc");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private CustomerStyleProfile buildProfile(String userId) {
        CustomerStyleProfile p = new CustomerStyleProfile();
        p.setUserId(userId);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return p;
    }

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
