package com.stylemind.product.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {
    private List<Long> categoryIds;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 200, message = "Tên sản phẩm tối đa 200 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Giá cơ sở không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal basePrice;

    @Pattern(regexp = "MALE|FEMALE|UNISEX", message = "Đối tượng mục tiêu phải là MALE, FEMALE hoặc UNISEX")
    @Builder.Default
    private String targetDemographic = "UNISEX";

    @Pattern(regexp = "ACTIVE|INACTIVE|DISCONTINUED", message = "Trạng thái phải là ACTIVE, INACTIVE hoặc DISCONTINUED")
    @Builder.Default
    private String status = "ACTIVE";
}