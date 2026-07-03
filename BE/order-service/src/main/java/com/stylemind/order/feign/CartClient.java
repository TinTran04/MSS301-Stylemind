package com.stylemind.order.feign;

import com.stylemind.cart.dto.CartMergeRequest;
import com.stylemind.cart.dto.CartResponse;
import com.stylemind.common.dto.ApiResponse;
import com.stylemind.order.config.ResilientReadFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "cart-service", url = "${CART_SERVICE_URL:http://localhost:8086}", configuration = ResilientReadFeignConfig.class)
public interface CartClient {

    @GetMapping("/api/v1/cart")
    ApiResponse<CartResponse> getCart(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader(value = "X-Guest-Session-Id", required = false) String guestSessionId);

    @PostMapping("/api/v1/cart/merge")
    ApiResponse<CartResponse> mergeCart(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CartMergeRequest request);

    @DeleteMapping("/api/v1/cart")
    ApiResponse<Void> clearCart(@RequestHeader("Authorization") String authHeader);

    // Used by the payment webhook path, which has no end-user Authorization header
    // to forward (SePay calls us server-to-server, not through the user's browser).
    @DeleteMapping("/internal/v1/cart/users/{userId}")
    ApiResponse<Void> clearCartByUserId(@PathVariable("userId") String userId);
}
