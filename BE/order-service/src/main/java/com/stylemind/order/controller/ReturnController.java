package com.stylemind.order.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.order.dto.*;
import com.stylemind.order.service.ReturnEligibilityService;
import com.stylemind.order.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;
    private final ReturnEligibilityService eligibilityService;

    @GetMapping("/orders/{orderId}/returns/eligibility")
    public ApiResponse<ReturnEligibilityResponse> evaluateEligibility(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("orderId") String orderId) {
        ReturnEligibilityResponse response = eligibilityService.evaluateEligibility(userId, orderId);
        return ApiResponse.success(response);
    }

    @PostMapping("/orders/{orderId}/returns")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReturnResponse> createReturnRequest(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("orderId") String orderId,
            @Valid @RequestBody CreateReturnRequest request) {
        ReturnResponse response = returnService.createReturnRequest(userId, orderId, request);
        return ApiResponse.success(response);
    }

    @GetMapping("/orders/{orderId}/returns")
    public ApiResponse<List<ReturnResponse>> getCustomerReturns(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("orderId") String orderId) {
        List<ReturnResponse> response = returnService.getCustomerReturns(userId, orderId);
        return ApiResponse.success(response);
    }

    @GetMapping("/returns/{returnId}")
    public ApiResponse<ReturnResponse> getCustomerReturnById(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("returnId") String returnId) {
        ReturnResponse response = returnService.getCustomerReturnById(userId, returnId);
        return ApiResponse.success(response);
    }

    @PostMapping("/returns/{returnId}/cancel")
    public ApiResponse<ReturnResponse> cancelReturnRequest(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("returnId") String returnId) {
        ReturnResponse response = returnService.cancelReturnRequest(userId, returnId);
        return ApiResponse.success(response);
    }

    @PostMapping("/returns/{returnId}/shipment")
    public ApiResponse<ReturnResponse> submitShipment(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("returnId") String returnId,
            @Valid @RequestBody SubmitShipmentRequest request) {
        ReturnResponse response = returnService.submitShipment(userId, returnId, request);
        return ApiResponse.success(response);
    }
}
