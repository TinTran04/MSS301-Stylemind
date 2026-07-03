package com.stylemind.order.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.order.service.OrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Service-to-service only - called by payment-service after it reconciles a SePay
// webhook (or immediately for a FAILED reconciliation). Guarded by
// InternalAuthFilter/SecurityConfig's /internal/v1/** rules and unreachable
// through the gateway (see api-gateway's internal-block route).
@RestController
@RequestMapping("/internal/v1/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderService orderService;

    @PostMapping("/{orderId}/payment-status")
    public ResponseEntity<ApiResponse<Void>> updatePaymentStatus(
            @PathVariable String orderId,
            @RequestBody PaymentStatusUpdateRequest request) {
        orderService.updateOrderStatusFromPayment(orderId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("OK", null));
    }

    @Data
    public static class PaymentStatusUpdateRequest {
        private String status;
    }
}
