package com.stylemind.cart.controller;

import com.stylemind.cart.dto.CartItemRequest;
import com.stylemind.cart.dto.CartMergeRequest;
import com.stylemind.cart.dto.CartResponse;
import com.stylemind.cart.service.CartService;
import com.stylemind.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.stylemind.common.security.UserPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    private String getGuestSessionId(HttpServletRequest request) {
        return request.getHeader("X-Guest-Session-Id");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        String userId = principal != null ? principal.getUserId() : null;
        String guestSessionId = getGuestSessionId(request);
        CartResponse cart = cartService.getCart(userId, guestSessionId);
        return ResponseEntity.ok(ApiResponse.success("Lấy giỏ hàng thành công", cart));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request,
            @Valid @RequestBody CartItemRequest itemRequest) {
        String userId = principal != null ? principal.getUserId() : null;
        String guestSessionId = getGuestSessionId(request);
        CartResponse cart = cartService.addItem(userId, guestSessionId, itemRequest);
        return ResponseEntity.ok(ApiResponse.success("Thêm sản phẩm vào giỏ hàng thành công", cart));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateQuantity(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request,
            @PathVariable String itemId,
            @RequestParam Integer quantity) {
        String userId = principal != null ? principal.getUserId() : null;
        String guestSessionId = getGuestSessionId(request);
        CartResponse cart = cartService.updateQuantity(userId, guestSessionId, itemId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật số lượng thành công", cart));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request,
            @PathVariable String itemId) {
        String userId = principal != null ? principal.getUserId() : null;
        String guestSessionId = getGuestSessionId(request);
        cartService.removeItem(userId, guestSessionId, itemId);
        return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm khỏi giỏ hàng thành công", null));
    }

    @PostMapping("/merge")
    public ResponseEntity<ApiResponse<CartResponse>> mergeCart(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CartMergeRequest mergeRequest) {
        if (principal == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("AUTH_REQUIRED", "Cần đăng nhập để gộp giỏ hàng")
            );
        }
        CartResponse cart = cartService.mergeCart(principal.getUserId(), mergeRequest);
        return ResponseEntity.ok(ApiResponse.success("Gộp giỏ hàng thành công", cart));
    }
}