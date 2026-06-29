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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stylemind.common.event.UserRegisteredEvent;
import com.stylemind.user.entity.Address;
import com.stylemind.user.repository.AddressRepository;
import com.stylemind.user.repository.ProcessedEventRepository;
import com.stylemind.user.repository.UserProfileRepository;
import com.stylemind.user.service.UserProfileService;
import com.stylemind.user.service.UserRegisteredEventHandler;
import java.time.Instant;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProfileGatewayJourneyTest {

    private static final String INTERNAL_TOKEN = "test-internal-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRegisteredEventHandler userRegisteredEventHandler;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private AddressRepository addressRepository;

    @BeforeEach
    void cleanDatabase() {
        addressRepository.deleteAll();
        processedEventRepository.deleteAll();
        userProfileRepository.deleteAll();
    }

    @Test
    void gatewayBackedProfileAndAddressJourney() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = userRegistered(eventId, userId, "Nguyen Van A");

        userRegisteredEventHandler.handle(event);
        userRegisteredEventHandler.handle(event);

        assertThat(processedEventRepository.count()).as("duplicate USER_REGISTERED must be idempotent").isEqualTo(1);
        assertThat(userProfileRepository.count()).as("duplicate USER_REGISTERED must not create duplicate profile").isEqualTo(1);

        mockMvc.perform(get("/api/users/me").headers(gatewayHeaders(userId, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId", is(userId.toString())))
                .andExpect(jsonPath("$.data.fullName", is("Nguyen Van A")));

        mockMvc.perform(patch("/api/users/me")
                        .headers(gatewayHeaders(userId, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": " Nguyen Van B ",
                                  "phone": "+84901234567",
                                  "avatarUrl": "https://cdn.example.com/avatar.png",
                                  "gender": "MALE",
                                  "dateOfBirth": "1995-01-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName", is("Nguyen Van B")))
                .andExpect(jsonPath("$.data.phone", is("+84901234567")));

        String firstAddressId = createAddress(userId, "First Receiver", false);
        String secondAddressId = createAddress(userId, "Second Receiver", false);

        mockMvc.perform(put("/api/users/me/addresses/{addressId}/default", secondAddressId)
                        .headers(gatewayHeaders(userId, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.addressId", is(secondAddressId)))
                .andExpect(jsonPath("$.data.isDefault", is(true)));

        List<Address> addresses = addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtAsc(userId);
        assertThat(addresses).hasSize(2);
        assertThat(addresses.stream().filter(Address::isDefaultAddress)).hasSize(1);
        assertThat(addresses.get(0).getId().toString()).isEqualTo(secondAddressId);
        assertThat(firstAddressId).isNotEqualTo(secondAddressId);

        mockMvc.perform(get("/api/users/me/addresses").headers(gatewayHeaders(userId, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].addressId", is(secondAddressId)))
                .andExpect(jsonPath("$.data[0].isDefault", is(true)));
    }

    @Test
    void userCannotAccessAnotherUsersAddressThroughGatewayContext() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID attackerId = UUID.randomUUID();
        userRegisteredEventHandler.handle(userRegistered(UUID.randomUUID(), ownerId, "Owner"));
        userRegisteredEventHandler.handle(userRegistered(UUID.randomUUID(), attackerId, "Attacker"));
        String ownerAddressId = createAddress(ownerId, "Owner Receiver", false);

        mockMvc.perform(get("/api/users/me/addresses").headers(gatewayHeaders(attackerId, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(patch("/api/users/me/addresses/{addressId}", ownerAddressId)
                        .headers(gatewayHeaders(attackerId, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverName\":\"Changed\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("ADDRESS_NOT_FOUND")));

        mockMvc.perform(delete("/api/users/me/addresses/{addressId}", ownerAddressId)
                        .headers(gatewayHeaders(attackerId, "CUSTOMER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("ADDRESS_NOT_FOUND")));
    }

    @Test
    void concurrentSetDefaultThroughServiceKeepsSingleDefaultAddress() throws Exception {
        UUID userId = UUID.randomUUID();
        userRegisteredEventHandler.handle(userRegistered(UUID.randomUUID(), userId, "Nguyen Van A"));
        String firstAddressId = createAddress(userId, "First Receiver", false);
        String secondAddressId = createAddress(userId, "Second Receiver", false);
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
        assertThat(addresses.stream().filter(Address::isDefaultAddress))
                .as("concurrent set-default must leave at most one default address")
                .hasSize(1);
        assertThat(failures.get()).isLessThanOrEqualTo(1);
    }

    private String createAddress(UUID userId, String receiverName, boolean isDefault) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users/me/addresses")
                        .headers(gatewayHeaders(userId, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverName": "%s",
                                  "receiverPhone": "+84901234567",
                                  "province": "Ho Chi Minh",
                                  "district": "District 1",
                                  "ward": "Ben Nghe",
                                  "streetAddress": "123 Nguyen Hue",
                                  "isDefault": %s
                                }
                                """.formatted(receiverName, isDefault)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.at("/data/addressId").asText();
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

    private org.springframework.http.HttpHeaders gatewayHeaders(UUID userId, String role) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("X-Internal-Request", "true");
        headers.add("X-Internal-Token", INTERNAL_TOKEN);
        headers.add("X-User-Id", userId.toString());
        headers.add("X-User-Role", role);
        return headers;
    }

    private UserRegisteredEvent userRegistered(UUID eventId, UUID userId, String fullName) {
        return new UserRegisteredEvent(
                eventId,
                "USER_REGISTERED",
                Instant.now(),
                new UserRegisteredEvent.UserRegisteredData(userId, fullName));
    }
}
