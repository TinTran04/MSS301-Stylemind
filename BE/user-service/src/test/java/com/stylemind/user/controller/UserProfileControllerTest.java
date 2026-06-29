package com.stylemind.user.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stylemind.common.security.JwtUtil;
import com.stylemind.common.security.UserPrincipal;
import com.stylemind.user.entity.UserProfile;
import com.stylemind.user.repository.UserProfileRepository;
import java.time.LocalDate;
import java.util.UUID;
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
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @BeforeEach
    void setUp() {
        userProfileRepository.deleteAll();
    }

    @Test
    void getProfileReturnsCurrentUserProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        userProfileRepository.save(UserProfile.builder()
                .userId(userId)
                .fullName("Nguyen Van A")
                .phone("+84901234567")
                .gender("MALE")
                .dateOfBirth(LocalDate.of(1995, 1, 1))
                .build());

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", bearerToken(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(userId.toString())))
                .andExpect(jsonPath("$.data.fullName", is("Nguyen Van A")))
                .andExpect(jsonPath("$.data.phone", is("+84901234567")));
    }

    @Test
    void getProfileReturnsNotFoundWhenProfileDoesNotExist() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", bearerToken(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("PROFILE_NOT_FOUND")));
    }

    @Test
    void patchProfileUpdatesOnlyCurrentUserProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        userProfileRepository.save(UserProfile.builder()
                .userId(userId)
                .fullName("Old Name")
                .phone("0901234567")
                .build());

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": " Nguyen Van A ",
                                  "phone": "+84987654321",
                                  "avatarUrl": "https://cdn.example.com/avatar.png",
                                  "gender": "MALE",
                                  "dateOfBirth": "1995-01-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(userId.toString())))
                .andExpect(jsonPath("$.data.fullName", is("Nguyen Van A")))
                .andExpect(jsonPath("$.data.phone", is("+84987654321")))
                .andExpect(jsonPath("$.data.avatarUrl", is("https://cdn.example.com/avatar.png")));
    }

    @Test
    void patchProfileDoesNotAllowClientSuppliedUserId() throws Exception {
        UUID userId = UUID.randomUUID();
        userProfileRepository.save(UserProfile.builder()
                .userId(userId)
                .fullName("Current User")
                .build());

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "00000000-0000-0000-0000-000000000000",
                                  "fullName": "Hacker"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    void patchProfileDoesNotAllowRoleOrEmail() throws Exception {
        UUID userId = UUID.randomUUID();
        userProfileRepository.save(UserProfile.builder()
                .userId(userId)
                .fullName("Current User")
                .build());

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "customer@example.com",
                                  "role": "ADMIN",
                                  "fullName": "Nguyen Van A"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    void patchProfileDoesNotUpdateAnotherUserProfile() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        userProfileRepository.save(UserProfile.builder()
                .userId(ownerId)
                .fullName("Owner")
                .phone("0901234567")
                .build());

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", bearerToken(otherUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Changed"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("PROFILE_NOT_FOUND")));

        UserProfile ownerProfile = userProfileRepository.findById(ownerId).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(ownerProfile.getFullName()).isEqualTo("Owner");
    }

    @Test
    void patchProfileRejectsInvalidPhone() throws Exception {
        UUID userId = UUID.randomUUID();
        userProfileRepository.save(UserProfile.builder()
                .userId(userId)
                .fullName("Current User")
                .build());

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "abc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    void patchProfileRejectsFutureDateOfBirth() throws Exception {
        UUID userId = UUID.randomUUID();
        userProfileRepository.save(UserProfile.builder()
                .userId(userId)
                .fullName("Current User")
                .build());

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dateOfBirth": "2999-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    private String bearerToken(UUID userId) {
        UserDetails userDetails = new UserPrincipal(userId.toString(), userId.toString(), "", "CUSTOMER", "JWT", true);
        return "Bearer " + jwtUtil.generateAccessToken(userDetails, userId.toString(), "CUSTOMER");
    }
}
