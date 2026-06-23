package com.stylemind.ai.service;

import com.stylemind.ai.dto.*;
import com.stylemind.ai.entity.*;
import com.stylemind.ai.repository.*;
import com.stylemind.ai.feign.*;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AiChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final AiCuratedBundleRepository bundleRepository;
    private final AiCuratedBundleItemRepository bundleItemRepository;
    private final AiAnalyticsLogRepository analyticsLogRepository;
    private final AiIndexJobRepository indexJobRepository;
    
    private final ProductClient productClient;
    private final OrderClient orderClient;

    public ChatResponse chat(ChatRequest request, String userId) {
        // Return mock data directly for frontend testing
        UUID sessionId = request.getConversationId();
        if (sessionId == null) {
            sessionId = UUID.randomUUID();
        }

        List<ChatResponse.RecommendedProduct> recommendations = new ArrayList<>();
        List<String> styleTips = new ArrayList<>();
        ChatResponse.CuratedBundle curatedBundle = null;
        String intent = detectIntent(request.getMessage());
        String aiResponse = "";

        if ("product_recommendation".equals(intent) || "outfit_recommendation".equals(intent)) {
            recommendations.add(ChatResponse.RecommendedProduct.builder()
                    .productId("P001")
                    .name("Áo Thun Mock Data")
                    .basePrice(new BigDecimal("150000"))
                    .imageUrl("https://example.com/mock-image.jpg")
                    .reason("Mock data reason")
                    .matchScore(0.95)
                    .build());
            
            styleTips.add("Mock style tip 1");
            styleTips.add("Mock style tip 2");

            curatedBundle = ChatResponse.CuratedBundle.builder()
                    .id(StringUtil.generateUniqueId())
                    .justificationSummary("Mock bundle justification")
                    .items(recommendations.stream()
                            .map(rec -> ChatResponse.CuratedBundle.BundleItem.builder()
                                    .productId(rec.getProductId())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();
            aiResponse = "Đây là kết quả mock data cho yêu cầu gợi ý sản phẩm.";
        } else if ("order_tracking".equals(intent)) {
            aiResponse = "Đơn hàng mock của bạn đang ở trạng thái: ĐANG_GIAO";
        } else {
             aiResponse = "Xin chào! Tôi là AI Stylist mock. Tôi có thể giúp gì cho bạn?";
        }


        return ChatResponse.builder()
                .conversationId(sessionId)
                .messageId(StringUtil.generateUniqueId())
                .senderType("AI")
                .messageText(aiResponse)
                .hasProductBlock(!recommendations.isEmpty())
                .intent(intent)
                .recommendedProducts(recommendations)
                .styleTips(styleTips)
                .curatedBundle(curatedBundle)
                .build();
    }

    private String detectIntent(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("đơn") && (lower.contains("thế nào") || lower.contains("đến đâu") || lower.contains("trạng thái"))) {
            return "order_tracking";
        }
        if (lower.contains("phối") || lower.contains("outfit") || lower.contains("bộ")) {
            return "outfit_recommendation";
        }
        if (lower.contains("gợi ý") || lower.contains("tư vấn") || lower.contains("tìm") || lower.contains("muốn mua")) {
            return "product_recommendation";
        }
        return "general_chat";
    }

    private String extractOrderId(String message) {
        // Simple regex to extract order ID
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("ORD-\\d{4}-\\d+");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private List<String> hybridSearch(String query, String userId) {
        // TODO: Implement actual hybrid search with:
        // 1. Vector Search (Qdrant) - semantic similarity
        // 2. Keyword Search (PostgreSQL Full Text) - exact matches
        // 3. Graph Traversal (Neo4j) - fashion rules
        // 4. Metadata Filter - inventory, price range, user preferences
        // 5. Re-ranking with weights: Vector(0.35) + Keyword(0.25) + Graph(0.25) + Personalization(0.15)
        
        // For now, return mock product IDs
        return Arrays.asList("P001", "P101", "P102", "P003", "P005");
    }

    private ChatResponse.RecommendedProduct buildRecommendation(String productId) {
        try {
            var productResponse = productClient.getProduct(productId);
            if (!productResponse.isSuccess() || productResponse.getData() == null) {
                return null;
            }

            ProductClient.ProductDetail product = productResponse.getData();

            String primaryImage = product.getImages().stream()
                    .filter(ProductClient.ImageDetail::getIsPrimary)
                    .findFirst()
                    .map(ProductClient.ImageDetail::getImageUrl)
                    .orElse(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl());

            return ChatResponse.RecommendedProduct.builder()
                    .productId(product.getId())
                    .name(product.getName())
                    .basePrice(product.getBasePrice())
                    .imageUrl(primaryImage)
                    .reason(generateReason(product))
                    .matchScore(0.92) // TODO: Calculate actual score
                    .build();
        } catch (Exception ex) {
            log.warn("Failed to build recommendation for product: {}", productId, ex);
            return null;
        }
    }

    private String generateReason(ProductClient.ProductDetail product) {
        return String.format("Sản phẩm %s phù hợp với yêu cầu của bạn. Giá: %,.0f VNĐ", 
                product.getName(), product.getBasePrice());
    }

    private List<String> generateStyleTips(String message, List<ChatResponse.RecommendedProduct> recommendations) {
        List<String> tips = new ArrayList<>();
        if (!recommendations.isEmpty()) {
            tips.add("Kết hợp với phụ kiện phù hợp để hoàn thiện trang phục");
            tips.add("Chọn size đúng chuẩn để đảm bảo form dáng đẹp nhất");
        }
        return tips;
    }

    private ChatResponse.CuratedBundle createCuratedBundle(UUID sessionId, List<ChatResponse.RecommendedProduct> recommendations) {
        String bundleId = StringUtil.generateUniqueId();
        
        AiCuratedBundle bundle = AiCuratedBundle.builder()
                .id(bundleId)
                .messageId("") // Will be updated after AI message is saved
                .justificationSummary("Bộ trang phục được AI gợi ý dựa trên nhu cầu và sở thích của bạn")
                .build();
        bundleRepository.save(bundle);

        for (ChatResponse.RecommendedProduct rec : recommendations) {
            AiCuratedBundleItem item = AiCuratedBundleItem.builder()
                    .bundleId(bundleId)
                    .productId(rec.getProductId())
                    .build();
            bundleItemRepository.save(item);
        }

        // Log IMPRESSION analytics
        logInteraction(null, bundleId, "IMPRESSION");

        return ChatResponse.CuratedBundle.builder()
                .id(bundleId)
                .justificationSummary(bundle.getJustificationSummary())
                .items(recommendations.stream()
                        .map(rec -> ChatResponse.CuratedBundle.BundleItem.builder()
                                .productId(rec.getProductId())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private String generateResponse(String message, String intent, List<ChatResponse.RecommendedProduct> recommendations, List<String> styleTips) {
        StringBuilder response = new StringBuilder();
        
        switch (intent) {
            case "product_recommendation":
                response.append("Dựa trên yêu cầu của bạn, tôi gợi ý một số sản phẩm phù hợp:");
                break;
            case "outfit_recommendation":
                response.append("Tôi đã phối cho bạn một bộ trang phục hoàn chỉnh:");
                break;
            case "order_tracking":
                response.append("Đang tra cứu thông tin đơn hàng...");
                break;
            default:
                response.append("Xin chào! Tôi có thể giúp gì cho bạn hôm nay?");
        }
        
        return response.toString();
    }

    private ChatResponse saveAndReturnAiResponse(ChatSession session, ChatMessage userMessage, 
                                                  String responseText, String intent,
                                                  List<ChatResponse.RecommendedProduct> recommendations,
                                                  List<String> styleTips,
                                                  ChatResponse.CuratedBundle curatedBundle) {
        
        // Save AI message
        ChatMessage aiMessage = ChatMessage.builder()
                .id(StringUtil.generateUniqueId())
                .sessionId(session.getId())
                .senderType("AI")
                .messageText(responseText)
                .hasProductBlock(recommendations != null && !recommendations.isEmpty())
                .build();
        messageRepository.save(aiMessage);

        // Update bundle with message_id if exists
        if (curatedBundle != null) {
            AiCuratedBundle bundle = bundleRepository.findById(curatedBundle.getId()).orElse(null);
            if (bundle != null) {
                bundle.setMessageId(aiMessage.getId());
                bundleRepository.save(bundle);
            }
        }

        // Log analytics
        if (curatedBundle != null) {
            logInteraction(session.getUserId(), curatedBundle.getId(), "IMPRESSION");
        }

        return ChatResponse.builder()
                .conversationId(session.getId())
                .messageId(aiMessage.getId())
                .senderType("AI")
                .messageText(responseText)
                .hasProductBlock(aiMessage.getHasProductBlock())
                .intent(intent)
                .recommendedProducts(recommendations)
                .styleTips(styleTips)
                .curatedBundle(curatedBundle)
                .build();
    }

    private void logInteraction(String userId, String bundleId, String interactionType) {
        AiAnalyticsLog log = AiAnalyticsLog.builder()
                .id(StringUtil.generateUniqueId())
                .userId(userId)
                .bundleId(bundleId)
                .interactionType(interactionType)
                .build();
        analyticsLogRepository.save(log);
    }

    public void logRecommendationInteraction(String userId, String bundleId, String interactionType) {
        logInteraction(userId, bundleId, interactionType);
    }

    public List<ChatResponse> getHistory(String userId) {
        return sessionRepository.findByUserId(userId).stream()
                .map(session -> {
                    List<ChatMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
                    return ChatResponse.builder()
                            .conversationId(session.getId())
                            .build(); // Simplified
                })
                .collect(Collectors.toList());
    }

    // Index management
    public void triggerReindex(String targetType, String targetId) {
        AiIndexJob job = AiIndexJob.builder()
                .id(StringUtil.generateUniqueId())
                .targetType(targetType)
                .targetId(targetId)
                .operationType("UPDATE")
                .status("PENDING")
                .build();
        indexJobRepository.save(job);
    }
}
