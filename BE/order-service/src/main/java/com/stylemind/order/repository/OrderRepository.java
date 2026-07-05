package com.stylemind.order.repository;

import com.stylemind.order.entity.Order;
import com.stylemind.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByUserId(String userId);
    List<Order> findByOrderStatus(OrderStatus orderStatus);
    List<Order> findByOrderStatusAndCreatedAtBefore(OrderStatus orderStatus, LocalDateTime cutoff);
    Page<Order> findByUserId(String userId, Pageable pageable);
    Optional<Order> findByIdAndUserId(String id, String userId);

    // ─── Admin dashboard aggregates (counts/sums only — no entities loaded) ───
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus IN :statuses")
    long countByStatuses(@Param("statuses") Collection<OrderStatus> statuses);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :from")
    long countCreatedSince(@Param("from") LocalDateTime from);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderStatus IN :statuses")
    BigDecimal sumRevenueByStatuses(@Param("statuses") Collection<OrderStatus> statuses);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderStatus IN :statuses AND o.createdAt >= :from")
    BigDecimal sumRevenueByStatusesSince(@Param("statuses") Collection<OrderStatus> statuses,
                                         @Param("from") LocalDateTime from);

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