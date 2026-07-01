package com.stylemind.order.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.security.UserPrincipal;
import com.stylemind.order.dto.*;
import com.stylemind.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request,
            @Valid @RequestBody CreateOrderRequest orderRequest) {
        String authHeader = request.getHeader("Authorization");
        OrderResponse order = orderService.createOrder(principal.getUserId(), authHeader, orderRequest);
        return ResponseEntity.ok(ApiResponse.success("Order created successfully", order));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(@AuthenticationPrincipal UserPrincipal principal) {
        List<OrderResponse> orders = orderService.getOrders(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Orders fetched successfully", orders));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String orderId) {
        OrderResponse order = orderService.getOrder(principal.getUserId(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Order fetched successfully", order));
    }

    @PostMapping("/{orderId}/payment/confirm")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request,
            @PathVariable String orderId,
            @Valid @RequestBody ConfirmPaymentRequest confirmRequest) {
        String authHeader = request.getHeader("Authorization");
        OrderResponse order = orderService.confirmOnlinePayment(principal.getUserId(), authHeader, orderId, confirmRequest);
        return ResponseEntity.ok(ApiResponse.success("Payment confirmed successfully", order));
    }

}
