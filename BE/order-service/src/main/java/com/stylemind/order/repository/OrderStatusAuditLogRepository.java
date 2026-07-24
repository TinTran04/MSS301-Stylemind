package com.stylemind.order.repository;

import com.stylemind.order.entity.OrderStatus;
import com.stylemind.order.entity.OrderStatusAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderStatusAuditLogRepository extends JpaRepository<OrderStatusAuditLog, String> {
    List<OrderStatusAuditLog> findByOrderIdOrderByCreatedAtAsc(String orderId);
    Optional<OrderStatusAuditLog> findFirstByOrderIdAndToStatusOrderByCreatedAtAsc(String orderId, OrderStatus toStatus);
}
