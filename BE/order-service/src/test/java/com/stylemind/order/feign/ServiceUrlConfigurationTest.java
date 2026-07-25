package com.stylemind.order.feign;

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

    @Test
    void dockerComposeUsesTheCanonicalInternalTokenForAuthAndOrder() throws IOException {
        Path composeFile = findRepositoryFile("docker-compose.yml");
        String compose = Files.readString(composeFile, StandardCharsets.UTF_8);

        String authServiceBlock = compose.substring(
                compose.indexOf("\n  auth-service:"),
                compose.indexOf("\n  user-service:"));
        String orderServiceBlock = compose.substring(
                compose.indexOf("\n  order-service:"),
                compose.indexOf("\n  payment-service:"));

        assertThat(authServiceBlock)
                .contains("INTERNAL_TOKEN: ${INTERNAL_TOKEN}")
                .doesNotContain("INTERNAL_TOKEN: ${X_INTERNAL_TOKEN}");
        assertThat(orderServiceBlock)
                .contains("INTERNAL_TOKEN: ${INTERNAL_TOKEN}");
    }

    @Test
    void dockerComposeGivesUserServiceTheCanonicalInternalTokenForAddressLookup() throws IOException {
        Path composeFile = findRepositoryFile("docker-compose.yml");
        String compose = Files.readString(composeFile, StandardCharsets.UTF_8);

        String userServiceBlock = compose.substring(
                compose.indexOf("\n  user-service:"),
                compose.indexOf("\n  product-service:"));

        assertThat(userServiceBlock)
                .as("user-service must validate Order Service address lookups with the same token")
                .contains("INTERNAL_TOKEN: ${INTERNAL_TOKEN}");
    }

    // notification-service's application.yml binds internal.token to X_INTERNAL_TOKEN (not
    // INTERNAL_TOKEN like auth-service/order-service), so its container must receive the same
    // configured value under that variable name - otherwise ORDER_PAID / registration-OTP emails
    // are rejected with 403 by InternalAuthFilter even though DNS/connectivity succeed.
    @Test
    void dockerComposeGivesNotificationServiceTheSameInternalTokenValueAsOrderAndAuth() throws IOException {
        Path composeFile = findRepositoryFile("docker-compose.yml");
        String compose = Files.readString(composeFile, StandardCharsets.UTF_8);

        String notificationServiceBlock = compose.substring(
                compose.indexOf("\n  notification-service:"),
                compose.indexOf("\n  ai-agent-service:"));

        assertThat(notificationServiceBlock)
                .as("notification-service's X_INTERNAL_TOKEN must be sourced from the same "
                        + "canonical INTERNAL_TOKEN value that auth-service/order-service send, "
                        + "not a separate X_INTERNAL_TOKEN .env value")
                .contains("X_INTERNAL_TOKEN: ${INTERNAL_TOKEN}");
    }

    @Test
    void dockerComposeGivesPaymentServiceTheVariableReadByItsInternalAuthFilter() throws IOException {
        Path composeFile = findRepositoryFile("docker-compose.yml");
        String compose = Files.readString(composeFile, StandardCharsets.UTF_8);

        String paymentServiceBlock = compose.substring(
                compose.indexOf("\n  payment-service:"),
                compose.indexOf("\n  notification-service:"));

        assertThat(paymentServiceBlock)
                .as("payment-service application.yml reads X_INTERNAL_TOKEN")
                .contains("X_INTERNAL_TOKEN: ${INTERNAL_TOKEN}");
    }

    // The internal-token RequestInterceptor is registered as a plain @Configuration bean in
    // common-lib and is scanned into every service's main ApplicationContext (see each
    // *Application.java's scanBasePackages), so it applies globally to every Feign client -
    // NotificationClient must not narrow its @FeignClient(configuration = ...) in a way that
    // would exclude it.
    @Test
    void notificationClient_doesNotOverrideFeignConfigurationAwayFromTheGlobalInterceptor() {
        assertThat(NotificationClient.class.getAnnotation(FeignClient.class).configuration())
                .isEmpty();
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
