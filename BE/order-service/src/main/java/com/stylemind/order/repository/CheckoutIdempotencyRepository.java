package com.stylemind.order.repository;

import com.stylemind.order.entity.CheckoutIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CheckoutIdempotencyRepository extends JpaRepository<CheckoutIdempotency, String> {
    Optional<CheckoutIdempotency> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);
}
