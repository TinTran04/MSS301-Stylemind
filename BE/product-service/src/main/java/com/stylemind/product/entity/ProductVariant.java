package com.stylemind.product.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_variants")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "product_id", length = 50, nullable = false)
    private String productId;

    @Column(name = "sku", length = 100, unique = true, nullable = false)
    private String sku;

    @Column(name = "size", length = 20, nullable = false)
    private String size;

    @Column(name = "color", length = 50, nullable = false)
    private String color;

    @Column(name = "material", length = 50)
    private String material;

    @Column(name = "price_override", precision = 12, scale = 2)
    private java.math.BigDecimal priceOverride;
}