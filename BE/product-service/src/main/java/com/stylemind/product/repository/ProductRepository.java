package com.stylemind.product.repository;

import com.stylemind.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    Optional<Product> findByIdAndStatus(String id, String status);
    List<Product> findByCategoryIdAndStatus(Long categoryId, String status);
    Page<Product> findByCategoryIdAndStatus(Long categoryId, String status, Pageable pageable);
    Page<Product> findByStatus(String status, Pageable pageable);
    List<Product> findByIdIn(List<String> ids);
    
    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', ?1, '%')) OR " +
           " LOWER(p.description) LIKE LOWER(CONCAT('%', ?1, '%')) OR " +
           " LOWER(p.aestheticStyle) LIKE LOWER(CONCAT('%', ?1, '%')))")
    List<Product> search(String keyword);
}