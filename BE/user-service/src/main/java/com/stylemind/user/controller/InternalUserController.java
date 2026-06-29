package com.stylemind.user.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.user.dto.DeliveryAddressResponse;
import com.stylemind.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal endpoints for service-to-service communication.
 * Protected by InternalAuthFilter (X-Internal-Token header required).
 * NOT accessible via the public API Gateway path.
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserProfileService userProfileService;

    /**
     * Allows order-service (or any internal service) to look up a delivery address
     * and verify it belongs to the specified user before placing an order.
     *
     * @param userId    the owner of the address (from internal caller, not JWT)
     * @param addressId the delivery address ID
     * @return 200 with address details, 404 if not found, 403 if userId mismatch
     */
    @GetMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<ApiResponse<DeliveryAddressResponse>> getAddress(
            @PathVariable String userId,
            @PathVariable String addressId) {
        DeliveryAddressResponse address = userProfileService.getAddressById(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success("OK", address));
    }
}
