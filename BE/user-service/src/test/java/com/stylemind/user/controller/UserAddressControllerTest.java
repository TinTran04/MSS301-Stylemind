package com.stylemind.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stylemind.common.security.JwtUtil;
import com.stylemind.common.security.UserPrincipal;
import com.stylemind.user.dto.AddressRequest;
import com.stylemind.user.dto.AddressResponse;
import com.stylemind.user.entity.Address;
import com.stylemind.user.repository.AddressRepository;
import com.stylemind.user.service.UserProfileService;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();
    }

    @Test
    void createAddressCreatesDefaultForFirstAddress() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/users/me/addresses")
                        .header("Authorization", bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addressJson("Nguyen Van A", "+84901234567", "Ho Chi Minh", "District 1", "Ben Nghe", "123 Nguyen Hue", false)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.receiverName", is("Nguyen Van A")))
                .andExpect(jsonPath("$.data.receiverPhone", is("+84901234567")))
                .andExpect(jsonPath("$.data.province", is("Ho Chi Minh")))
                .andExpect(jsonPath("$.data.isDefault", is(true)));
    }

    @Test
    void createMultipleAddressesKeepsOnlyOneDefault() throws Exception {
        UUID userId = UUID.randomUUID();

        createAddress(userId, "First", false);
        createAddress(userId, "Second", true);
        createAddress(userId, "Third", false);

        List<Address> addresses = addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtAsc(userId);
        assertThat(addresses).hasSize(3);
        assertThat(addresses.stream().filter(Address::isDefaultAddress)).hasSize(1);
        assertThat(addresses.stream().filter(Address::isDefaultAddress).findFirst().orElseThrow().getReceiverName())
                .isEqualTo("Second");

        mockMvc.perform(get("/api/users/me/addresses")
                        .header("Authorization", bearerToken(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].receiverName", is("Second")))
                .andExpect(jsonPath("$.data[0].isDefault", is(true)));
    }

    @Test
    void updateAddressUpdatesOwnedAddress() throws Exception {
        UUID userId = UUID.randomUUID();
        String addressId = createAddress(userId, "Receiver", false).getAddressId();

        mockMvc.perform(patch("/api/users/me/addresses/{addressId}", addressId)
                        .header("Authorization", bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverName": " Tran Van B ",
                                  "receiverPhone": "+84987654321",
                                  "streetAddress": " 456 Le Loi "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.receiverName", is("Tran Van B")))
                .andExpect(jsonPath("$.data.receiverPhone", is("+84987654321")))
                .andExpect(jsonPath("$.data.streetAddress", is("456 Le Loi")));
    }

    @Test
    void deleteAddressDeletesOwnedAddressAndPromotesNextDefault() throws Exception {
        UUID userId = UUID.randomUUID();
        String firstAddressId = createAddress(userId, "First", false).getAddressId();
        createAddress(userId, "Second", false);

        mockMvc.perform(delete("/api/users/me/addresses/{addressId}", firstAddressId)
                        .header("Authorization", bearerToken(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        List<Address> addresses = addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtAsc(userId);
        assertThat(addresses).hasSize(1);
        assertThat(addresses.get(0).isDefaultAddress()).isTrue();
        assertThat(addresses.get(0).getReceiverName()).isEqualTo("Second");
    }

    @Test
    void setDefaultAddressKeepsOnlyOneDefault() throws Exception {
        UUID userId = UUID.randomUUID();
        createAddress(userId, "First", false);
        String secondAddressId = createAddress(userId, "Second", false).getAddressId();

        mockMvc.perform(put("/api/users/me/addresses/{addressId}/default", secondAddressId)
                        .header("Authorization", bearerToken(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.addressId", is(secondAddressId)))
                .andExpect(jsonPath("$.data.isDefault", is(true)));

        List<Address> addresses = addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtAsc(userId);
        assertThat(addresses.stream().filter(Address::isDefaultAddress)).hasSize(1);
        assertThat(addresses.get(0).getId().toString()).isEqualTo(secondAddressId);
    }

    @Test
    void cannotAccessAnotherUsersAddress() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        String ownerAddressId = createAddress(ownerId, "Owner", false).getAddressId();

        mockMvc.perform(get("/api/users/me/addresses")
                        .header("Authorization", bearerToken(otherUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(patch("/api/users/me/addresses/{addressId}", ownerAddressId)
                        .header("Authorization", bearerToken(otherUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverName": "Changed"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("ADDRESS_NOT_FOUND")));

        mockMvc.perform(put("/api/users/me/addresses/{addressId}/default", ownerAddressId)
                        .header("Authorization", bearerToken(otherUserId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("ADDRESS_NOT_FOUND")));

        mockMvc.perform(delete("/api/users/me/addresses/{addressId}", ownerAddressId)
                        .header("Authorization", bearerToken(otherUserId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("ADDRESS_NOT_FOUND")));
    }

    @Test
    void validationRejectsMissingAndInvalidFields() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/users/me/addresses")
                        .header("Authorization", bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverName": "",
                                  "receiverPhone": "abc",
                                  "province": "Ho Chi Minh",
                                  "district": "District 1",
                                  "ward": "Ben Nghe",
                                  "streetAddress": "123 Nguyen Hue"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    void concurrentSetDefaultDoesNotCreateTwoDefaultAddresses() throws Exception {
        UUID userId = UUID.randomUUID();
        String firstAddressId = createAddress(userId, "First", false).getAddressId();
        String secondAddressId = createAddress(userId, "Second", false).getAddressId();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger failures = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> setDefaultConcurrently(userId, firstAddressId, ready, start, failures));
        executor.submit(() -> setDefaultConcurrently(userId, secondAddressId, ready, start, failures));
        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        List<Address> addresses = addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtAsc(userId);
        assertThat(addresses.stream().filter(Address::isDefaultAddress)).hasSize(1);
        assertThat(failures.get()).isLessThanOrEqualTo(1);
    }

    private void setDefaultConcurrently(
            UUID userId,
            String addressId,
            CountDownLatch ready,
            CountDownLatch start,
            AtomicInteger failures) {
        try {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            userProfileService.setMyDefaultAddress(userId.toString(), addressId);
        } catch (Exception ex) {
            failures.incrementAndGet();
        }
    }

    private AddressResponse createAddress(UUID userId, String receiverName, boolean isDefault) {
        AddressRequest request = new AddressRequest();
        request.setReceiverName(receiverName);
        request.setReceiverPhone("+84901234567");
        request.setProvince("Ho Chi Minh");
        request.setDistrict("District 1");
        request.setWard("Ben Nghe");
        request.setStreetAddress("123 Nguyen Hue");
        request.setIsDefault(isDefault);
        return userProfileService.createMyAddress(userId.toString(), request);
    }

    private String addressJson(
            String receiverName,
            String receiverPhone,
            String province,
            String district,
            String ward,
            String streetAddress,
            boolean isDefault) {
        return """
                {
                  "receiverName": "%s",
                  "receiverPhone": "%s",
                  "province": "%s",
                  "district": "%s",
                  "ward": "%s",
                  "streetAddress": "%s",
                  "isDefault": %s
                }
                """.formatted(receiverName, receiverPhone, province, district, ward, streetAddress, isDefault);
    }

    private String bearerToken(UUID userId) {
        UserDetails userDetails = new UserPrincipal(userId.toString(), userId.toString(), "", "CUSTOMER", "JWT", true);
        return "Bearer " + jwtUtil.generateAccessToken(userDetails, userId.toString(), "CUSTOMER");
    }
}
