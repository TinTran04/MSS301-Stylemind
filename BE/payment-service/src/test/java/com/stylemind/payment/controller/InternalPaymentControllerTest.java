package com.stylemind.payment.controller;

import com.stylemind.common.exception.GlobalExceptionHandler;
import com.stylemind.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalPaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalPaymentController(paymentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void validIsoRangeReachesPaymentService() throws Exception {
        LocalDateTime from = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 8, 1, 0, 0);
        when(paymentService.findSepayRevenueCandidates(from, to)).thenReturn(List.of());

        mockMvc.perform(get("/internal/v1/payments/admin/revenue/sepay")
                        .param("from", "2026-07-01T00:00:00")
                        .param("to", "2026-08-01T00:00:00"))
                .andExpect(status().isOk());

        verify(paymentService).findSepayRevenueCandidates(eq(from), eq(to));
    }

    @Test
    void missingFromReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/internal/v1/payments/admin/revenue/sepay")
                        .param("to", "2026-08-01T00:00:00"))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).findSepayRevenueCandidates(any(), any());
    }

    @Test
    void missingToReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/internal/v1/payments/admin/revenue/sepay")
                        .param("from", "2026-07-01T00:00:00"))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).findSepayRevenueCandidates(any(), any());
    }

    @Test
    void invalidDateReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/internal/v1/payments/admin/revenue/sepay")
                        .param("from", "not-a-date")
                        .param("to", "2026-08-01T00:00:00"))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).findSepayRevenueCandidates(any(), any());
    }

    @Test
    void nonIncreasingRangeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/internal/v1/payments/admin/revenue/sepay")
                        .param("from", "2026-08-01T00:00:00")
                        .param("to", "2026-07-01T00:00:00"))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).findSepayRevenueCandidates(any(), any());
    }
}
