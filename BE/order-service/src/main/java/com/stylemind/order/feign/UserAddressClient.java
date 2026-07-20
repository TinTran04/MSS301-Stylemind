package com.stylemind.order.feign;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.feign.FeignClientConfig;
import com.stylemind.order.config.ResilientReadFeignConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${USER_SERVICE_URL:http://user-service:8082}",
        configuration = {ResilientReadFeignConfig.class, FeignClientConfig.class})
public interface UserAddressClient {

    @GetMapping("/internal/v1/users/{userId}/addresses/{addressId}")
    ApiResponse<DeliveryAddressSnapshot> getAddress(
            @PathVariable("userId") String userId,
            @PathVariable("addressId") String addressId);

    @Getter
    @Setter
    class DeliveryAddressSnapshot {
        private String id;
        private String userId;
        private String recipientName;
        private String phoneNumber;
        private String provinceCode;
        private String provinceName;
        private String wardCode;
        private String wardName;
        private String addressLine;
        private String shippingNote;
        private String validationStatus;
        private String administrativeDataVersion;
    }
}
