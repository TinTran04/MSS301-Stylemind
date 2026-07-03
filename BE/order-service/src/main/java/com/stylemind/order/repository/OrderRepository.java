package com.stylemind.order.repository;

import com.stylemind.order.entity.Order;
import com.stylemind.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByUserId(String userId);
    List<Order> findByOrderStatus(OrderStatus orderStatus);
    List<Order> findByOrderStatusAndCreatedAtBefore(OrderStatus orderStatus, LocalDateTime cutoff);
    Page<Order> findByUserId(String userId, Pageable pageable);
    Optional<Order> findByIdAndUserId(String id, String userId);

    @Query("""
            SELECT o FROM Order o
            WHERE (CAST(:status AS string) IS NULL OR o.orderStatus = :status)
              AND (:userId IS NULL OR o.userId = :userId)
              AND (CAST(:fromDate AS timestamp) IS NULL OR o.createdAt >= :fromDate)
              AND (CAST(:toDate AS timestamp) IS NULL OR o.createdAt <= :toDate)
            """)
    Page<Order> search(
            @Param("status") OrderStatus status,
            @Param("userId") String userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
}