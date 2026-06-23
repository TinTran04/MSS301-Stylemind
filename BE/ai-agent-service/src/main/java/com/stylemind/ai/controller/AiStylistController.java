package com.stylemind.ai.controller;

import com.stylemind.ai.dto.*;
import com.stylemind.ai.service.AiChatService;
import com.stylemind.ai.service.AiIndexJobService;
import com.stylemind.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.stylemind.common.security.UserPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/ai-stylist")
@RequiredArgsConstructor
public class AiStylistController {

    private final AiChatService aiChatService;
    private final AiIndexJobService indexJobService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChatRequest request) {
        
        String userId = principal != null ? principal.getUserId() : null;
        ChatResponse response = aiChatService.chat(request, userId);
        return ResponseEntity.ok(ApiResponse.success("AI tư vấn thành công", response));
    }

    @PostMapping("/explain")
    public ResponseEntity<ApiResponse<ExplainResponse>> explain(
            @Valid @RequestBody ExplainRequest request) {
        ExplainResponse response = ExplainResponse.builder()
                .productId(request.getProductId())
                .matchScore(0.95)
                .breakdown(ExplainResponse.Breakdown.builder()
                        .styleFit(0.9)
                        .colorHarmony(0.92)
                        .silhouetteCompatibility(0.96)
                        .build())
                .reasoningFactors(List.of("Màu sắc hài hòa", "Phom dáng năng động"))
                .build();
        return ResponseEntity.ok(ApiResponse.success("Phân tích mức độ phù hợp thành công", response));
    }

    @PostMapping("/recommend-outfits")
    public ResponseEntity<ApiResponse<RecommendOutfitsResponse>> recommendOutfits(
            @Valid @RequestBody RecommendOutfitsRequest request) {
        RecommendOutfitsResponse response = RecommendOutfitsResponse.builder()
                .outfits(List.of(
                        RecommendOutfitsResponse.Outfit.builder()
                                .id("O001")
                                .name("Outfit năng động ngày hè")
                                .description("Outfit mock năng động ngày hè")
                                .aestheticStyle("Casual")
                                .matchScore(0.94)
                                .reasoning("Phong cách trẻ trung")
                                .items(List.of(
                                        RecommendOutfitsResponse.OutfitItem.builder()
                                                .productId("P001")
                                                .name("Áo thun")
                                                .basePrice(new java.math.BigDecimal("150000"))
                                                .imageUrl("https://example.com/mock-image.jpg")
                                                .role("TOP")
                                                .build()
                                ))
                                .build()
                ))
                .build();
        return ResponseEntity.ok(ApiResponse.success("Gợi ý outfit thành công", response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ChatResponse>>> getHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ChatResponse> history = aiChatService.getHistory(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử hội thoại thành công", history));
    }

    @PostMapping("/analytics/interaction")
    public ResponseEntity<ApiResponse<Void>> logInteraction(
            @RequestParam String bundleId,
            @RequestParam String interactionType) {
        aiChatService.logRecommendationInteraction(null, bundleId, interactionType);
        return ResponseEntity.ok(ApiResponse.success("Ghi log tương tác thành công", null));
    }
}

// Additional response DTOs
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class ExplainResponse {
    private String productId;
    private Double matchScore;
    private Breakdown breakdown;
    private List<String> reasoningFactors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class Breakdown {
        private Double styleFit;
        private Double colorHarmony;
        private Double silhouetteCompatibility;
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class RecommendOutfitsResponse {
    private List<Outfit> outfits;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class Outfit {
        private String id;
        private String name;
        private String description;
        private String aestheticStyle;
        private Double matchScore;
        private String reasoning;
        private BigDecimal totalPrice;
        private List<OutfitItem> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class OutfitItem {
        private String productId;
        private String name;
        private BigDecimal basePrice;
        private String imageUrl;
        private String role;
    }
}
