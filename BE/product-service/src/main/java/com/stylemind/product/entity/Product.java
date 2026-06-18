package com.stylemind.product.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", precision = 12, scale = 2, nullable = false)
    private java.math.BigDecimal basePrice;

    @Column(name = "aesthetic_style", length = 50)
    private String aestheticStyle;

    @Column(name = "target_demographic", length = 20)
    private String targetDemographic;

    @Column(name = "seasonal_property", length = 20)
    private String seasonalProperty;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "ACTIVE";
}