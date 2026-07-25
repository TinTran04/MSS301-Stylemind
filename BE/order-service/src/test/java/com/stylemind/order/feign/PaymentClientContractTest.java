package com.stylemind.order.feign;

import feign.Feign;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.support.SpringMvcContract;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentClientContractTest {

    @Test
    void revenueRequestIncludesNamedIsoDateParameters() {
        AtomicReference<Request> capturedRequest = new AtomicReference<>();
        PaymentClient client = Feign.builder()
                .contract(new SpringMvcContract())
                .client((request, options) -> {
                    capturedRequest.set(request);
                    return Response.builder()
                            .status(204)
                            .reason("No Content")
                            .request(request)
                            .headers(Collections.emptyMap())
                            .body(new byte[0])
                            .build();
                })
                .target(PaymentClient.class, "http://payment-service:8088");

        client.findSepayRevenueCandidates("2026-07-01T00:00:00", "2026-08-01T00:00:00");

        assertThat(capturedRequest.get().url())
                .contains("/internal/v1/payments/admin/revenue/sepay")
                .contains("from=2026-07-01T00%3A00%3A00")
                .contains("to=2026-08-01T00%3A00%3A00");
    }
}
