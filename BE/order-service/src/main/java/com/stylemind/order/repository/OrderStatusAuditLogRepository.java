package com.stylemind.order.repository;

import com.stylemind.order.entity.OrderStatusAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusAuditLogRepository extends JpaRepository<OrderStatusAuditLog, String> {
    List<OrderStatusAuditLog> findByOrderIdOrderByCreatedAtAsc(String orderId);
}
