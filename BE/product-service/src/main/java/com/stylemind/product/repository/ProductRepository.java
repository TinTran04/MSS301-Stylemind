package com.stylemind.product.repository;

import com.stylemind.product.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(
            @org.springframework.data.repository.query.Param("id") String id);
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.status = 'ACTIVE' AND " +
           "EXISTS (SELECT v.id FROM ProductVariant v WHERE v.productId = p.id)")
    Optional<Product> findSellableById(
            @org.springframework.data.repository.query.Param("id") String id);
    List<Product> findByCategoryIdAndStatus(Long categoryId, String status);
    Page<Product> findByCategoryIdAndStatus(Long categoryId, String status, Pageable pageable);
    Page<Product> findByStatus(String status, Pageable pageable);
    List<Product> findByIdIn(List<String> ids);
    boolean existsByCategoryId(Long categoryId);
    long countByStatus(String status);
    
    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND " +
           "EXISTS (SELECT v.id FROM ProductVariant v WHERE v.productId = p.id) AND " +
           "(:categoryId IS NULL OR p.categoryId = :categoryId) AND " +
           "(:minPrice IS NULL OR p.basePrice >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.basePrice <= :maxPrice) AND " +
           "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')) OR " +
           " LOWER(p.description) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')) OR " +
           " LOWER(p.aestheticStyle) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')))")
    Page<Product> searchAndFilter(
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            @org.springframework.data.repository.query.Param("categoryId") Long categoryId,
            @org.springframework.data.repository.query.Param("minPrice") java.math.BigDecimal minPrice,
            @org.springframework.data.repository.query.Param("maxPrice") java.math.BigDecimal maxPrice,
            Pageable pageable);

    // Admin view: unlike searchAndFilter, deliberately does NOT restrict to ACTIVE — admins must
    // be able to see and manage INACTIVE/DISCONTINUED products too.
    @Query("SELECT p FROM Product p WHERE " +
           "(CAST(:status AS string) IS NULL OR p.status = :status) AND " +
           "(:categoryId IS NULL OR p.categoryId = :categoryId) AND " +
           "(CAST(:keyword AS string) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
           " LOWER(p.description) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))")
    Page<Product> searchAndFilterAdmin(
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            @org.springframework.data.repository.query.Param("categoryId") Long categoryId,
            @org.springframework.data.repository.query.Param("status") String status,
            Pageable pageable);
}
