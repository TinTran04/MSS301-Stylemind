package com.stylemind.payment.repository;

import com.stylemind.payment.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    List<Transaction> findByOrderId(String orderId);
    List<Transaction> findByUserId(String userId);
    Optional<Transaction> findByTransactionRef(String transactionRef);
}