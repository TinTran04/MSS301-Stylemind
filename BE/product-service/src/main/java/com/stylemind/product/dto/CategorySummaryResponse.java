package com.stylemind.product.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorySummaryResponse {
    private Long id;
    private String name;
}
