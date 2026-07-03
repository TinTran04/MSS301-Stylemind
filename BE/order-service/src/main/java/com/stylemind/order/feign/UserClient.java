package com.stylemind.order.feign;

import com.stylemind.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", url = "${auth.service.url:http://auth-service:8081}")
public interface UserClient {

    @GetMapping("/internal/v1/users/{userId}/email")
    ApiResponse<UserEmail> getUserEmail(@PathVariable String userId);

    class UserEmail {
        private String userId;
        private String email;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
