package com.stylemind.payment.repository;

import com.stylemind.payment.entity.RefundTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, String> {
    Optional<RefundTransaction> findByOrderId(String orderId);
    Optional<RefundTransaction> findByOrderCancellationId(String orderCancellationId);
}
