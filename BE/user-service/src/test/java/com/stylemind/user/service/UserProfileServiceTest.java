package com.stylemind.user.service;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import com.stylemind.user.dto.DeliveryAddressRequest;
import com.stylemind.user.dto.DeliveryAddressResponse;
import com.stylemind.user.dto.StyleProfileRequest;
import com.stylemind.user.dto.StyleProfileResponse;
import com.stylemind.user.entity.CustomerStyleProfile;
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

    @InjectMocks
    private UserProfileService userProfileService;

    // ─── Style Profile ────────────────────────────────────────────────────────

    @Test
    void getStyleProfile_noProfileExists_returnsEmptyResponseWithoutPersisting() {
        when(profileRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        StyleProfileResponse result = userProfileService.getStyleProfile("user-1");

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo("user-1");
        assertThat(result.getGender()).isNull();
        // CRITICAL: must NOT save anything to the database on a GET
        verify(profileRepository, never()).save(any());
    }

    @Test
    void getStyleProfile_profileExists_returnsMappedResponse() {
        CustomerStyleProfile profile = buildProfile("user-1");
        profile.setGender("MALE");
        profile.setAge(25);
        when(profileRepository.findByUserId("user-1")).thenReturn(Optional.of(profile));

        StyleProfileResponse result = userProfileService.getStyleProfile("user-1");

        assertThat(result.getGender()).isEqualTo("MALE");
        assertThat(result.getAge()).isEqualTo(25);
        verify(profileRepository, never()).save(any());
    }

    @Test
    void updateStyleProfile_newUser_createsAndSavesProfile() {
        when(profileRepository.findByUserId("new-user")).thenReturn(Optional.empty());
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
        when(profileRepository.findByUserId("user-1")).thenReturn(Optional.of(existing));
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
                .city("Hanoi")
                .isDefault(isDefault)
                .build();
    }
}
