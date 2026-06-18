package com.stylemind.order.feign;

import com.stylemind.cart.dto.CartMergeRequest;
import com.stylemind.cart.dto.CartResponse;
import com.stylemind.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "cart-service", url = "${CART_SERVICE_URL:http://localhost:8086}")
public interface CartClient {

    @GetMapping("/api/cart")
    ApiResponse<CartResponse> getCart(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader(value = "X-Guest-Session-Id", required = false) String guestSessionId);

    @PostMapping("/api/cart/merge")
    ApiResponse<CartResponse> mergeCart(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CartMergeRequest request);
}