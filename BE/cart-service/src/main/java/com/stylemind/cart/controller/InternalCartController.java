package com.stylemind.cart.controller;

import com.stylemind.cart.dto.CartResponse;
import com.stylemind.cart.service.CartService;
import com.stylemind.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/cart")
@RequiredArgsConstructor
public class InternalCartController {

    private final CartService cartService;

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<CartResponse>> getCartByUserId(@PathVariable String userId) {
        CartResponse cart = cartService.getCart(userId, null);
        return ResponseEntity.ok(ApiResponse.success("OK", cart));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> clearCartByUserId(@PathVariable String userId) {
        cartService.clearCart(userId, null);
        return ResponseEntity.ok(ApiResponse.success("OK", null));
    }
}
