package com.stylemind.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BundleResponse {
    private String id;
    private String justificationSummary;
    private List<RecommendedProduct> items;
    private Instant createdAt;
}
