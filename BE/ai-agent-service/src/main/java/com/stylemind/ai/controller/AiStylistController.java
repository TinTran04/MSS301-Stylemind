package com.stylemind.ai.controller;

import com.stylemind.ai.dto.BundleResponse;
import com.stylemind.ai.dto.ChatRequest;
import com.stylemind.ai.dto.ChatResponse;
import com.stylemind.ai.service.AiChatService;
import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai-stylist")
@RequiredArgsConstructor
public class AiStylistController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChatRequest request) {
        ChatResponse response = aiChatService.chat(request, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("AI stylist replied", response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ChatResponse>>> getHistory(@AuthenticationPrincipal UserPrincipal principal) {
        List<ChatResponse> history = aiChatService.getHistory(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Chat history fetched", history));
    }

    @GetMapping("/bundles")
    public ResponseEntity<ApiResponse<List<BundleResponse>>> getBundles(@AuthenticationPrincipal UserPrincipal principal) {
        List<BundleResponse> bundles = aiChatService.getBundles(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Curated bundles fetched", bundles));
    }
}
