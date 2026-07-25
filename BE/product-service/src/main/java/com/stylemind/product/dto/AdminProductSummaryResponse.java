package com.stylemind.product.dto;

import lombok.*;

/** Real product catalogue counts for the admin dashboard. Counts only. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminProductSummaryResponse {
    private long totalProducts;
    private long activeProducts;
    private long inactiveProducts; // any non-ACTIVE status (INACTIVE/DISCONTINUED/…)
}
