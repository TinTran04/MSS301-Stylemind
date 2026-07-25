package com.stylemind.payment.feign;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceUrlConfigurationTest {

    @Test
    void orderClient_requiresExplicitOrderServiceUrl() {
        assertThat(OrderClient.class.getAnnotation(FeignClient.class).url())
                .isEqualTo("${ORDER_SERVICE_URL}");
    }

    @Test
    void applicationConfig_requiresOrderServiceUrl() throws IOException {
        String config = new String(
                new ClassPathResource("application.yml").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(config).doesNotContain("localhost:8087");
        assertThat(config).doesNotContain("ORDER_SERVICE_URL:http://");
    }

    @Test
    void dockerComposeInjectsOrderServiceUrlIntoPaymentService() throws IOException {
        Path composeFile = findRepositoryFile("docker-compose.yml");
        String compose = Files.readString(composeFile, StandardCharsets.UTF_8);

        String paymentServiceBlock = compose.substring(
                compose.indexOf("\n  payment-service:"),
                compose.indexOf("\n  notification-service:"));

        assertThat(paymentServiceBlock)
                .contains("ORDER_SERVICE_URL: ${ORDER_SERVICE_URL}")
                .doesNotContain("ORDER_SERVICE_URL: http://localhost:8087");
    }

    private static Path findRepositoryFile(String fileName) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(fileName);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to find " + fileName);
    }
}
