package com.stylemind.product.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantRequest {
    @NotBlank(message = "SKU không được để trống")
    @Size(max = 100, message = "SKU tối đa 100 ký tự")
    private String sku;

    @NotBlank(message = "Size không được để trống")
    @Size(max = 20, message = "Size tối đa 20 ký tự")
    private String size;

    @NotBlank(message = "Màu sắc không được để trống")
    @Size(max = 50, message = "Màu sắc tối đa 50 ký tự")
    private String color;

    @Size(max = 50, message = "Chất liệu tối đa 50 ký tự")
    private String material;

    private BigDecimal priceOverride;
}