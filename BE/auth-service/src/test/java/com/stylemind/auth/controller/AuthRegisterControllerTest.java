package com.stylemind.auth.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stylemind.auth.repository.AccountRepository;
import com.stylemind.auth.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void registerEndpointCreatesAccountWithoutExposingPasswordHash() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": " Customer@Example.COM ",
                                  "password": "StrongPassword123!",
                                  "fullName": "Nguyen Van A"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("customer@example.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(content().string(not(containsString("password_hash"))))
                .andExpect(content().string(not(containsString("passwordHash"))))
                .andExpect(content().string(not(containsString("StrongPassword123"))));
    }

    @Test
    void registerEndpointRejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "StrongPassword123!",
                                  "fullName": "Nguyen Van A"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void registerEndpointRejectsPasswordLongerThan128Characters() throws Exception {
        String longPassword = "A".repeat(129);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "customer@example.com",
                                  "password": "%s",
                                  "fullName": "Nguyen Van A"
                                }
                                """.formatted(longPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
