package com.stylemind.product.repository;

import com.stylemind.product.entity.ProductCategory;
import com.stylemind.product.entity.ProductCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, ProductCategoryId> {
    List<ProductCategory> findByProductId(String productId);
    List<ProductCategory> findByProductIdIn(List<String> productIds);
    void deleteByProductId(String productId);
    boolean existsByCategoryId(Long categoryId);
}
