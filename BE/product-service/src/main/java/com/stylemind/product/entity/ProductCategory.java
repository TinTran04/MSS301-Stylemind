package com.stylemind.product.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ProductCategoryId.class)
public class ProductCategory {

    @Id
    @Column(name = "product_id", length = 50)
    private String productId;

    @Id
    @Column(name = "category_id")
    private Long categoryId;
}
