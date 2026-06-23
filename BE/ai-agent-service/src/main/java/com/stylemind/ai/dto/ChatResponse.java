package com.stylemind.ai.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {
    private UUID conversationId;
    private String messageId;
    private String senderType;
    private String messageText;
    private Boolean hasProductBlock;
    private String intent;
    private List<RecommendedProduct> recommendedProducts;
    private List<String> styleTips;
    private CuratedBundle curatedBundle;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendedProduct {
        private String productId;
        private String name;
        private BigDecimal basePrice;
        private String imageUrl;
        private String reason;
        private Double matchScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CuratedBundle {
        private String id;
        private String justificationSummary;
        private List<BundleItem> items;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class BundleItem {
            private String productId;
        }
    }
}
