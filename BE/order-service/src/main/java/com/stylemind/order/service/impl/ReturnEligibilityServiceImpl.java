package com.stylemind.order.service.impl;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.order.dto.ReturnEligibilityResponse;
import com.stylemind.order.entity.Order;
import com.stylemind.order.entity.OrderItem;
import com.stylemind.order.entity.OrderStatus;
import com.stylemind.order.entity.OrderStatusAuditLog;
import com.stylemind.order.repository.OrderItemRepository;
import com.stylemind.order.repository.OrderRepository;
import com.stylemind.order.repository.OrderStatusAuditLogRepository;
import com.stylemind.order.repository.ReturnItemRepository;
import com.stylemind.order.service.ReturnEligibilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReturnEligibilityServiceImpl implements ReturnEligibilityService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusAuditLogRepository auditLogRepository;
    private final ReturnItemRepository returnItemRepository;

    @Override
    public ReturnEligibilityResponse evaluateEligibility(String userId, String orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", 404));

        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            return ReturnEligibilityResponse.builder()
                    .eligible(false)
                    .reasonCode("RETURN_ORDER_NOT_COMPLETED")
                    .message("Đơn hàng chưa hoàn tất, chưa thể yêu cầu trả hàng.")
                    .build();
        }

        LocalDateTime completedAt = findCompletedAt(orderId);
        if (completedAt == null) {
            return ReturnEligibilityResponse.builder()
                    .eligible(false)
                    .reasonCode("RETURN_COMPLETION_TIME_NOT_FOUND")
                    .message("Không tìm thấy thời điểm hoàn tất đơn hàng trong lịch sử hệ thống.")
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime returnWindowEnd = completedAt.plusDays(30);

        if (now.isBefore(completedAt)) {
            return ReturnEligibilityResponse.builder()
                    .eligible(false)
                    .reasonCode("RETURN_WINDOW_NOT_STARTED")
                    .message("Thời hạn yêu cầu trả hàng chưa bắt đầu.")
                    .completedAt(completedAt)
                    .returnWindowEnd(returnWindowEnd)
                    .build();
        }

        if (!now.isBefore(returnWindowEnd)) { // now >= completedAt + 30 days
            return ReturnEligibilityResponse.builder()
                    .eligible(false)
                    .reasonCode("RETURN_WINDOW_EXPIRED")
                    .message("Thời hạn 30 ngày yêu cầu trả hàng đã hết.")
                    .completedAt(completedAt)
                    .returnWindowEnd(returnWindowEnd)
                    .build();
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        Map<String, Integer> remainingMap = new HashMap<>();
        boolean anyQuantityLeft = false;

        for (OrderItem item : items) {
            int remaining = computeRemainingReturnableQuantity(orderId, item.getId(), item.getQuantity());
            remainingMap.put(item.getId(), remaining);
            if (remaining > 0) {
                anyQuantityLeft = true;
            }
        }

        if (!anyQuantityLeft) {
            return ReturnEligibilityResponse.builder()
                    .eligible(false)
                    .reasonCode("RETURN_QUANTITY_EXCEEDS_AVAILABLE")
                    .message("Tất cả sản phẩm trong đơn hàng đã được yêu cầu trả hàng trước đó.")
                    .completedAt(completedAt)
                    .returnWindowEnd(returnWindowEnd)
                    .remainingReturnableQuantities(remainingMap)
                    .build();
        }

        return ReturnEligibilityResponse.builder()
                .eligible(true)
                .reasonCode("ELIGIBLE")
                .message("Đơn hàng đủ điều kiện yêu cầu trả hàng.")
                .completedAt(completedAt)
                .returnWindowEnd(returnWindowEnd)
                .remainingReturnableQuantities(remainingMap)
                .build();
    }

    @Override
    public LocalDateTime findCompletedAt(String orderId) {
        return auditLogRepository.findFirstByOrderIdAndToStatusOrderByCreatedAtAsc(orderId, OrderStatus.COMPLETED)
                .map(OrderStatusAuditLog::getCreatedAt)
                .orElse(null);
    }

    @Override
    public Integer computeRemainingReturnableQuantity(String orderId, String orderItemId, Integer orderedQuantity) {
        Integer reservedOrConsumed = returnItemRepository.sumReservedOrConsumedQuantity(orderId, orderItemId);
        int reserved = reservedOrConsumed != null ? reservedOrConsumed : 0;
        return Math.max(0, orderedQuantity - reserved);
    }
}
