package com.stylemind.order.service;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import com.stylemind.order.entity.Order;
import com.stylemind.order.entity.OrderStatus;
import com.stylemind.order.entity.OrderStatusAuditLog;
import com.stylemind.order.event.OrderStatusChangedEvent;
import com.stylemind.order.exception.InvalidOrderStatusTransitionException;
import com.stylemind.order.repository.OrderRepository;
import com.stylemind.order.repository.OrderStatusAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// The single funnel for every order status write. Nothing outside this
// class may call Order.setOrderStatus() directly (see order.setOrderStatus
// usages in OrderService, which all now route through here).
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderStatusService {

    private final OrderRepository orderRepository;
    private final OrderStatusAuditLogRepository auditLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Order changeStatus(String orderId, OrderStatus target, String actorId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", 404));
        return changeStatus(order, target, actorId);
    }

    public Order changeStatus(Order order, OrderStatus target, String actorId) {
        OrderStatus current = order.getOrderStatus();
        if (!current.canTransitionTo(target)) {
            throw new InvalidOrderStatusTransitionException(current, target);
        }

        order.setOrderStatus(target);
        Order saved = orderRepository.save(order);
        recordAudit(actorId, order.getId(), current, target);
        eventPublisher.publishEvent(new OrderStatusChangedEvent(saved.getId(), saved.getUserId(), current, target));
        return saved;
    }

    private void recordAudit(String actorId, String orderId, OrderStatus from, OrderStatus to) {
        auditLogRepository.save(OrderStatusAuditLog.builder()
                .id(StringUtil.generateUniqueId())
                .orderId(orderId)
                .actorId(actorId)
                .fromStatus(from)
                .toStatus(to)
                .build());
        log.info("Order status change | actor={} orderId={} from={} to={}", actorId, orderId, from, to);
    }
}
