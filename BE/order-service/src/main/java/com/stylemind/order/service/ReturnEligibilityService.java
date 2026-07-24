package com.stylemind.order.service;

import com.stylemind.order.dto.ReturnEligibilityResponse;

import java.time.LocalDateTime;

public interface ReturnEligibilityService {
    ReturnEligibilityResponse evaluateEligibility(String userId, String orderId);
    LocalDateTime findCompletedAt(String orderId);
    Integer computeRemainingReturnableQuantity(String orderId, String orderItemId, Integer orderedQuantity);
}
