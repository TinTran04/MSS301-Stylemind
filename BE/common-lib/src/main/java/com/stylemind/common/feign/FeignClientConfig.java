package com.stylemind.common.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@RequiredArgsConstructor
public class FeignClientConfig {

    @Value("${internal.token:${INTERNAL_SERVICE_SECRET:${INTERNAL_TOKEN:change-me-local-internal-service-secret}}}")
    private String internalToken;

    @Bean
    public RequestInterceptor internalRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // Add internal token for service-to-service calls
                template.header("X-Internal-Token", internalToken);
                template.header("X-Internal-Request", "true");
                
                // Propagate X-Request-Id for distributed tracing
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    String requestId = request.getHeader("X-Request-Id");
                    if (requestId != null) {
                        template.header("X-Request-Id", requestId);
                    }
                    // Propagate user context headers if present
                    String userId = request.getHeader("X-User-Id");
                    if (userId != null) {
                        template.header("X-User-Id", userId);
                    }
                    String userRoles = request.getHeader("X-User-Roles");
                    if (userRoles != null) {
                        template.header("X-User-Roles", userRoles);
                    }
                }
            }
        };
    }
}
