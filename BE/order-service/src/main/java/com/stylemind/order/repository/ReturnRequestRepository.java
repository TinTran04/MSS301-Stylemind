package com.stylemind.order.repository;

import com.stylemind.order.entity.ReturnRequest;
import com.stylemind.order.entity.ReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, String> {

    List<ReturnRequest> findByOrderIdOrderByRequestedAtDesc(String orderId);

    List<ReturnRequest> findByUserIdAndOrderIdOrderByRequestedAtDesc(String userId, String orderId);

    Optional<ReturnRequest> findByIdAndUserId(String id, String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReturnRequest r WHERE r.id = :id")
    Optional<ReturnRequest> findByIdForUpdate(@Param("id") String id);

    Page<ReturnRequest> findByStatus(ReturnStatus status, Pageable pageable);

    long countByStatus(ReturnStatus status);

    @Query("SELECT r FROM ReturnRequest r WHERE r.orderId = :orderId AND r.status NOT IN ('REJECTED', 'CANCELLED', 'QC_FAILED')")
    List<ReturnRequest> findActiveReturnsByOrderId(@Param("orderId") String orderId);
}
