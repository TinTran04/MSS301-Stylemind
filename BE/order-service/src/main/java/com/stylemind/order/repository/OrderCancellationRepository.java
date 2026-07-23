package com.stylemind.order.repository;

import com.stylemind.order.entity.OrderCancellation;
import com.stylemind.order.entity.OrderCancellationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderCancellationRepository extends JpaRepository<OrderCancellation, String> {
    List<OrderCancellation> findByOrderIdOrderByCreatedAtDesc(String orderId);
    List<OrderCancellation> findByOrderIdAndStatus(String orderId, OrderCancellationStatus status);
    Optional<OrderCancellation> findFirstByOrderIdAndStatus(String orderId, OrderCancellationStatus status);
    boolean existsByOrderIdAndStatus(String orderId, OrderCancellationStatus status);
    Optional<OrderCancellation> findByIdAndStatus(String id, OrderCancellationStatus status);
    List<OrderCancellation> findByUserIdAndOrderIdOrderByCreatedAtDesc(String userId, String orderId);
    Optional<OrderCancellation> findByRequestedByAndOrderIdAndIdempotencyKey(String requestedBy, String orderId, String idempotencyKey);
    List<OrderCancellation> findByOrderIdInOrderByCreatedAtDesc(Collection<String> orderIds);
    List<OrderCancellation> findByStatus(OrderCancellationStatus status);
    long countByStatus(OrderCancellationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from OrderCancellation c where c.id = :id")
    Optional<OrderCancellation> findByIdForUpdate(@Param("id") String id);
}
