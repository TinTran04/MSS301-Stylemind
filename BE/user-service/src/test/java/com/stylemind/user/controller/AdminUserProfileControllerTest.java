package com.stylemind.user.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stylemind.user.entity.Address;
import com.stylemind.user.entity.UserProfile;
import com.stylemind.user.repository.AddressRepository;
import com.stylemind.user.repository.UserProfileRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private AddressRepository addressRepository;

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();
        userProfileRepository.deleteAll();
    }

    @Test
    void adminCanListProfiles() throws Exception {
        UUID userId = UUID.randomUUID();
        userProfileRepository.save(UserProfile.builder()
                .userId(userId)
                .fullName("Nguyen Van A")
                .phone("+84901234567")
                .build());

        mockMvc.perform(get("/api/admin/users")
                        .headers(gatewayHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].userId", is(userId.toString())))
                .andExpect(jsonPath("$.data.totalItems", is(1)))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void adminCanGetProfileWithAddresses() throws Exception {
        UUID userId = UUID.randomUUID();
        userProfileRepository.save(UserProfile.builder()
                .userId(userId)
                .fullName("Nguyen Van A")
                .phone("+84901234567")
                .build());
        addressRepository.save(Address.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .receiverName("Nguyen Van A")
                .receiverPhone("+84901234567")
                .province("Ho Chi Minh")
                .district("District 1")
                .ward("Ben Nghe")
                .streetAddress("1 Le Loi")
                .defaultAddress(true)
                .build());

        mockMvc.perform(get("/api/admin/users/{userId}", userId)
                        .headers(gatewayHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.profile.userId", is(userId.toString())))
                .andExpect(jsonPath("$.data.addresses", hasSize(1)))
                .andExpect(jsonPath("$.data.addresses[0].isDefault", is(true)))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    private HttpHeaders gatewayHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Internal-Request", "true");
        headers.add("X-Internal-Token", "test-internal-token");
        headers.add("X-User-Id", UUID.randomUUID().toString());
        headers.add("X-User-Role", "ADMIN");
        return headers;
    }
}
