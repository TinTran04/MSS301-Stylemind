package com.stylemind.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = {"com.stylemind.gateway", "com.stylemind.common"})
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth routes
                .route("auth-service", r -> r.path("/api/auth/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://auth-service"))

                // User routes
                .route("user-service", r -> r.path("/api/users/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://user-service"))

                // Product routes
                .route("product-service", r -> r.path("/api/products/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://product-service"))

                // Category routes
                .route("category-service", r -> r.path("/api/categories/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://product-service"))

                // Cart routes
                .route("cart-service", r -> r.path("/api/cart/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cart-service"))

                // Order routes
                .route("order-service", r -> r.path("/api/orders/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://order-service"))

                // Payment routes
                .route("payment-service", r -> r.path("/api/payment/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://payment-service"))

                // AI routes
                .route("ai-service", r -> r.path("/api/ai-stylist/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://ai-agent-service"))

                // Admin routes
                .route("admin-service", r -> r.path("/api/admin/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://product-service"))

                // Internal routes (blocked from external)
                .route("internal-block", r -> r.path("/internal/**")
                        .filters(f -> f.setStatus(401))
                        .uri("no://op"))

                .build();
    }
}