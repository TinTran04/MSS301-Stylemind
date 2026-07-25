package com.stylemind.product.repository;

import com.stylemind.product.entity.ProductAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAuditLogRepository extends JpaRepository<ProductAuditLog, String> {
}
