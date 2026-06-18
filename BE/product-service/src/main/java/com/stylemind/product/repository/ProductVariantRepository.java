package com.stylemind.product.repository;

import com.stylemind.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, String> {
    Optional<ProductVariant> findBySku(String sku);
    List<ProductVariant> findByProductId(String productId);
    List<ProductVariant> findByIdIn(List<String> ids);
    List<ProductVariant> findBySkuIn(List<String> skus);
}