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
    boolean existsByCategoryId(Long categoryId);
    
    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND " +
           "(:categoryId IS NULL OR p.categoryId = :categoryId) AND " +
           "(:minPrice IS NULL OR p.basePrice >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.basePrice <= :maxPrice) AND " +
           "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.aestheticStyle) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchAndFilter(
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            @org.springframework.data.repository.query.Param("categoryId") Long categoryId,
            @org.springframework.data.repository.query.Param("minPrice") java.math.BigDecimal minPrice,
            @org.springframework.data.repository.query.Param("maxPrice") java.math.BigDecimal maxPrice,
            Pageable pageable);
}