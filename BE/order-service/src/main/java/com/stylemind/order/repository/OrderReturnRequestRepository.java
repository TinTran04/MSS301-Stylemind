package com.stylemind.order.repository;

import com.stylemind.order.entity.OrderReturnRequest;
import com.stylemind.order.entity.OrderReturnStatus;
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
public interface OrderReturnRequestRepository extends JpaRepository<OrderReturnRequest, String> {
    boolean existsByOrderId(String orderId);
    List<OrderReturnRequest> findByOrderIdOrderByCreatedAtDesc(String orderId);
    List<OrderReturnRequest> findByUserIdAndOrderIdOrderByCreatedAtDesc(String userId, String orderId);
    List<OrderReturnRequest> findByOrderIdInOrderByCreatedAtDesc(Collection<String> orderIds);
    List<OrderReturnRequest> findByStatus(OrderReturnStatus status);
    List<OrderReturnRequest> findByStatusIn(Collection<OrderReturnStatus> statuses);
    Optional<OrderReturnRequest> findByRequestedByAndOrderIdAndIdempotencyKey(String requestedBy, String orderId, String idempotencyKey);
    long countByStatus(OrderReturnStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from OrderReturnRequest r where r.id = :id")
    Optional<OrderReturnRequest> findByIdForUpdate(@Param("id") String id);
}
