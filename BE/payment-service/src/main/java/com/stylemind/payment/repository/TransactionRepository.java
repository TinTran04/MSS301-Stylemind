package com.stylemind.payment.repository;

import com.stylemind.payment.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    List<Transaction> findByOrderId(String orderId);
    List<Transaction> findByUserId(String userId);
    Optional<Transaction> findByTransactionRef(String transactionRef);
    List<Transaction> findByMethodAndStatus(String method, String status);
    List<Transaction> findByMethodAndStatusIn(String method, Collection<String> statuses);
    List<Transaction> findByOrderIdIn(Collection<String> orderIds);

    @Query("""
            SELECT t FROM Transaction t
            WHERE UPPER(t.method) IN ('SEPAY', 'SEPAY_QR')
              AND UPPER(t.status) IN ('PAID', 'REFUNDED')
              AND (:fromTime IS NULL OR t.paidAt >= :fromTime)
              AND (:toTime IS NULL OR t.paidAt < :toTime)
            """)
    List<Transaction> findSepayRevenueCandidates(
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime);

    Optional<Transaction> findTopByOrderIdOrderByCreatedAtDesc(String orderId);
}
