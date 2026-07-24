package com.stylemind.product.repository;

import com.stylemind.product.entity.RestockLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RestockLogRepository extends JpaRepository<RestockLog, String> {
    Optional<RestockLog> findByOperationKey(String operationKey);
}
