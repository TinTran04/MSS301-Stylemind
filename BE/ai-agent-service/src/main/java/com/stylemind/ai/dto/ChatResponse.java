package com.stylemind.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {
    private UUID conversationId;
    private String messageId;
    private String senderType; // USER, AI
    private String messageText;
    private Boolean hasProductBlock;
    private List<RecommendedProduct> recommendedProducts;
    private String bundleId;
    private Instant createdAt;
}
