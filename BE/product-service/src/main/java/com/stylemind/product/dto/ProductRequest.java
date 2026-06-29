package com.stylemind.product.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {
    private Long categoryId;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 200, message = "Tên sản phẩm tối đa 200 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Giá cơ sở không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal basePrice;

    @Size(max = 50, message = "Phong cách tối đa 50 ký tự")
    private String aestheticStyle;

    @Size(max = 20, message = "Đối tượng mục tiêu tối đa 20 ký tự")
    private String targetDemographic;

    @Size(max = 20, message = "Mùa tối đa 20 ký tự")
    private String seasonalProperty;

    @Pattern(regexp = "ACTIVE|INACTIVE|DISCONTINUED", message = "Trạng thái phải là ACTIVE, INACTIVE hoặc DISCONTINUED")
    @Builder.Default
    private String status = "ACTIVE";
}