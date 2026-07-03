package com.stylemind.order.service;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.order.entity.Order;
import com.stylemind.order.entity.OrderStatus;
import com.stylemind.order.exception.InvalidOrderStatusTransitionException;
import com.stylemind.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

// The single funnel for every order status write. Nothing outside this
// class may call Order.setOrderStatus() directly (see order.setOrderStatus
// usages in OrderService, which all now route through here).
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderStatusService {

    private final OrderRepository orderRepository;

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
        return saved;
    }

    private void recordAudit(String actorId, String orderId, OrderStatus from, OrderStatus to) {
        // TODO: persist to a dedicated order_status_audit_log table (mirroring
        // auth-service's AuditLog) once the audit storage/retention requirements
        // for orders are finalized. Logging for now so every transition is at
        // least traceable.
        log.info("Order status change | actor={} orderId={} from={} to={} at={}",
                actorId, orderId, from, to, Instant.now());
    }
}
