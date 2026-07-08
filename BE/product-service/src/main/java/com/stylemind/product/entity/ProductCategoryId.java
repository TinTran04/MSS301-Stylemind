package com.stylemind.product.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryId implements Serializable {
    private String productId;
    private Long categoryId;
}
