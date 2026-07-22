package com.stylemind.order.repository;

import com.stylemind.order.entity.OrderStatusAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderStatusAuditLogRepository extends JpaRepository<OrderStatusAuditLog, String> {
    List<OrderStatusAuditLog> findByOrderIdOrderByCreatedAtAsc(String orderId);

    @Query("""
            SELECT DISTINCT a.orderId FROM OrderStatusAuditLog a
            WHERE a.toStatus = com.stylemind.order.entity.OrderStatus.COMPLETED
              AND a.createdAt >= :fromTime
              AND a.createdAt < :toTime
            """)
    List<String> findCompletedOrderIdsBetween(
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime);
}
