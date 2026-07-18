package com.stylemind.order.feign;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceUrlConfigurationTest {

    @Test
    void productClient_requiresExplicitProductServiceUrl() {
        assertThat(ProductClient.class.getAnnotation(FeignClient.class).url())
                .isEqualTo("${PRODUCT_SERVICE_URL}");
    }

    @Test
    void cartClient_requiresExplicitCartServiceUrl() {
        assertThat(CartClient.class.getAnnotation(FeignClient.class).url())
                .isEqualTo("${CART_SERVICE_URL}");
    }

    @Test
    void paymentClient_requiresExplicitPaymentServiceUrl() {
        assertThat(PaymentClient.class.getAnnotation(FeignClient.class).url())
                .isEqualTo("${PAYMENT_SERVICE_URL}");
    }

    @Test
    void applicationConfig_requiresAuthAndNotificationServiceUrls() throws IOException {
        String config = new String(
                new ClassPathResource("application.yml").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(config).contains("url: ${AUTH_SERVICE_URL}");
        assertThat(config).contains("url: ${NOTIFICATION_SERVICE_URL}");
        assertThat(config).doesNotContain("localhost:8081");
        assertThat(config).doesNotContain("localhost:8089");
    }
}
