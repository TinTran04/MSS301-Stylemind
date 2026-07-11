package com.stylemind.ai.service;

import com.stylemind.ai.dto.BundleResponse;
import com.stylemind.ai.dto.ChatRequest;
import com.stylemind.ai.dto.ChatResponse;
import com.stylemind.ai.dto.RecommendedProduct;
import com.stylemind.ai.entity.AiCuratedBundle;
import com.stylemind.ai.entity.AiCuratedBundleItem;
import com.stylemind.ai.entity.ChatMessage;
import com.stylemind.ai.entity.ChatSession;
import com.stylemind.ai.feign.ProductClient;
import com.stylemind.ai.repository.AiCuratedBundleItemRepository;
import com.stylemind.ai.repository.AiCuratedBundleRepository;
import com.stylemind.ai.repository.ChatMessageRepository;
import com.stylemind.ai.repository.ChatSessionRepository;
import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// MVP AI Stylist: rule-based keyword matching against real product-service data,
// not a real LLM/vector search yet (see docs/services/ai-agent-service.md - phase sau).
// Chat/history/bundle persistence is real so the endpoints and the eventual swap-in
// of real AI don't require frontend or DTO changes (see RecommendedProduct).
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AiChatService {

    private static final Map<String, StyleContext> STYLE_CONTEXTS = new HashMap<>();
    static {
        STYLE_CONTEXTS.put("dinner", new StyleContext("dinner",
                "For a dinner event, I recommend elegant pieces that transition from golden hour to evening.",
                List.of("dress", "elegant", "evening")));
        STYLE_CONTEXTS.put("casual", new StyleContext("casual",
                "For a relaxed everyday look, these pieces offer comfort without sacrificing style.",
                List.of("casual", "everyday")));
        STYLE_CONTEXTS.put("work", new StyleContext("work",
                "Professional pieces that command attention while staying comfortable for the office.",
                List.of("office", "professional", "work")));
        STYLE_CONTEXTS.put("summer", new StyleContext("summer",
                "Lightweight, breathable pieces perfect for warm weather.",
                List.of("summer", "light")));
    }

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final AiCuratedBundleRepository bundleRepository;
    private final AiCuratedBundleItemRepository bundleItemRepository;
    private final ProductClient productClient;

    public ChatResponse chat(ChatRequest request, String userId) {
        ChatSession session = resolveSession(request.getConversationId(), userId);

        ChatMessage userMessage = ChatMessage.builder()
                .id(StringUtil.generateUniqueId())
                .sessionId(session.getId())
                .senderType("USER")
                .messageText(request.getMessage())
                .hasProductBlock(false)
                .build();
        messageRepository.save(userMessage);

        StyleContext context = detectContext(request.getMessage());
        List<RecommendedProduct> recommendations = findRecommendations(context);

        String aiText = recommendations.isEmpty()
                ? "I couldn't find matching pieces right now, but here's my general style advice: " + context.reply()
                : context.reply();

        ChatMessage aiMessage = ChatMessage.builder()
                .id(StringUtil.generateUniqueId())
                .sessionId(session.getId())
                .senderType("AI")
                .messageText(aiText)
                .hasProductBlock(!recommendations.isEmpty())
                .build();
        messageRepository.save(aiMessage);

        String bundleId = null;
        if (!recommendations.isEmpty()) {
            bundleId = saveBundle(aiMessage.getId(), aiText, recommendations);
        }

        return ChatResponse.builder()
                .conversationId(session.getId())
                .messageId(aiMessage.getId())
                .senderType("AI")
                .messageText(aiText)
                .hasProductBlock(!recommendations.isEmpty())
                .recommendedProducts(recommendations)
                .bundleId(bundleId)
                .createdAt(java.time.Instant.now())
                .build();
    }

    public List<ChatResponse> getHistory(String userId) {
        List<ChatSession> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (sessions.isEmpty()) {
            return List.of();
        }

        List<UUID> sessionIds = sessions.stream().map(ChatSession::getId).collect(Collectors.toList());
        List<ChatMessage> messages = messageRepository.findBySessionIdInOrderByCreatedAtAsc(sessionIds);

        List<String> aiMessageIds = messages.stream()
                .filter(ChatMessage::getHasProductBlock)
                .map(ChatMessage::getId)
                .collect(Collectors.toList());
        Map<String, AiCuratedBundle> bundleByMessageId = aiMessageIds.isEmpty()
                ? Map.of()
                : bundleRepository.findByMessageIdInOrderByCreatedAtDesc(aiMessageIds).stream()
                        .collect(Collectors.toMap(AiCuratedBundle::getMessageId, b -> b, (a, b) -> a));

        return messages.stream()
                .map(msg -> {
                    AiCuratedBundle bundle = bundleByMessageId.get(msg.getId());
                    return ChatResponse.builder()
                            .conversationId(msg.getSessionId())
                            .messageId(msg.getId())
                            .senderType(msg.getSenderType())
                            .messageText(msg.getMessageText())
                            .hasProductBlock(msg.getHasProductBlock())
                            .recommendedProducts(bundle != null ? hydrateBundleItems(bundle.getId()) : List.of())
                            .bundleId(bundle != null ? bundle.getId() : null)
                            .createdAt(toInstant(msg.getCreatedAt()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<BundleResponse> getBundles(String userId) {
        List<ChatSession> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (sessions.isEmpty()) {
            return List.of();
        }
        List<UUID> sessionIds = sessions.stream().map(ChatSession::getId).collect(Collectors.toList());
        List<String> aiMessageIds = messageRepository.findBySessionIdInOrderByCreatedAtAsc(sessionIds).stream()
                .filter(ChatMessage::getHasProductBlock)
                .map(ChatMessage::getId)
                .collect(Collectors.toList());
        if (aiMessageIds.isEmpty()) {
            return List.of();
        }

        return bundleRepository.findByMessageIdInOrderByCreatedAtDesc(aiMessageIds).stream()
                .map(bundle -> BundleResponse.builder()
                        .id(bundle.getId())
                        .justificationSummary(bundle.getJustificationSummary())
                        .items(hydrateBundleItems(bundle.getId()))
                        .createdAt(toInstant(bundle.getCreatedAt()))
                        .build())
                .collect(Collectors.toList());
    }

    private ChatSession resolveSession(UUID conversationId, String userId) {
        if (conversationId == null) {
            return sessionRepository.save(ChatSession.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .build());
        }
        return sessionRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new BusinessException("CONVERSATION_NOT_FOUND", "Conversation not found", 404));
    }

    private StyleContext detectContext(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("dinner") || lower.contains("evening") || lower.contains("formal")) {
            return STYLE_CONTEXTS.get("dinner");
        }
        if (lower.contains("work") || lower.contains("office") || lower.contains("professional")) {
            return STYLE_CONTEXTS.get("work");
        }
        if (lower.contains("summer") || lower.contains("beach") || lower.contains("vacation")) {
            return STYLE_CONTEXTS.get("summer");
        }
        if (lower.contains("casual") || lower.contains("everyday") || lower.contains("relaxed")) {
            return STYLE_CONTEXTS.get("casual");
        }
        return new StyleContext("default",
                "Based on your style profile, here are a few pieces you might like.", List.of());
    }

    private List<RecommendedProduct> findRecommendations(StyleContext context) {
        try {
            String keyword = context.searchTerms().isEmpty() ? null : context.searchTerms().get(0);
            var response = productClient.getProducts(keyword, 0, 2);
            List<ProductClient.ProductSummary> products = response != null && response.isSuccess() && response.getData() != null
                    ? response.getData().getContent()
                    : List.of();

            if (products.isEmpty() && StringUtils.hasText(keyword)) {
                // Fallback: no keyword match, surface latest catalog items instead of nothing.
                var fallback = productClient.getProducts(null, 0, 2);
                products = fallback != null && fallback.isSuccess() && fallback.getData() != null
                        ? fallback.getData().getContent()
                        : List.of();
            }

            List<RecommendedProduct> result = new ArrayList<>();
            double score = 0.95;
            for (ProductClient.ProductSummary product : products) {
                result.add(RecommendedProduct.builder()
                        .productId(product.getId())
                        .name(product.getName())
                        .basePrice(product.getBasePrice())
                        .imageUrl(primaryImage(product))
                        .reason(String.format("Matches your \"%s\" request", context.label()))
                        .matchScore(score)
                        .build());
                score -= 0.06;
            }
            return result;
        } catch (Exception ex) {
            log.warn("product-service lookup failed for AI recommendation, returning empty list", ex);
            return List.of();
        }
    }

    private List<RecommendedProduct> hydrateBundleItems(String bundleId) {
        List<AiCuratedBundleItem> items = bundleItemRepository.findByBundleId(bundleId);
        List<RecommendedProduct> result = new ArrayList<>();
        for (AiCuratedBundleItem item : items) {
            try {
                var response = productClient.getProduct(item.getProductId());
                if (response != null && response.isSuccess() && response.getData() != null) {
                    ProductClient.ProductSummary product = response.getData();
                    result.add(RecommendedProduct.builder()
                            .productId(product.getId())
                            .name(product.getName())
                            .basePrice(product.getBasePrice())
                            .imageUrl(primaryImage(product))
                            .reason(null)
                            .matchScore(null)
                            .build());
                }
            } catch (Exception ex) {
                log.warn("product-service lookup failed while hydrating bundle {}, skipping item {}", bundleId, item.getProductId(), ex);
            }
        }
        return result;
    }

    private String saveBundle(String messageId, String justification, List<RecommendedProduct> recommendations) {
        String bundleId = StringUtil.generateUniqueId();
        bundleRepository.save(AiCuratedBundle.builder()
                .id(bundleId)
                .messageId(messageId)
                .justificationSummary(justification)
                .build());
        for (RecommendedProduct rec : recommendations) {
            bundleItemRepository.save(AiCuratedBundleItem.builder()
                    .bundleId(bundleId)
                    .productId(rec.getProductId())
                    .build());
        }
        return bundleId;
    }

    private String primaryImage(ProductClient.ProductSummary product) {
        if (product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }
        return product.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .findFirst()
                .map(ProductClient.ImageSummary::getImageUrl)
                .orElse(product.getImages().get(0).getImageUrl());
    }

    private java.time.Instant toInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    private record StyleContext(String label, String reply, List<String> searchTerms) {
    }
}
