package com.stylemind.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AiFallbackController {

    @PostMapping("/api/v1/ai-stylist/chat")
    public Mono<ResponseEntity<Map<String, Object>>> chat(@RequestBody Map<String, Object> request) {
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("response", "Xin chào! Hiện tại tính năng Tư vấn thời trang AI đang được bảo trì. Vui lòng quay lại sau.");
        responseData.put("conversationId", request.getOrDefault("conversationId", "mock-conversation-id"));

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("message", "Chat response generated successfully");
        body.put("data", responseData);

        return Mono.just(ResponseEntity.ok(body));
    }

    @GetMapping("/api/v1/ai-stylist/history")
    public Mono<ResponseEntity<Map<String, Object>>> getChatHistory() {
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("message", "Chat history retrieved");
        body.put("data", new ArrayList<>());

        return Mono.just(ResponseEntity.ok(body));
    }

    @GetMapping("/api/v1/ai-stylist/bundles")
    public Mono<ResponseEntity<Map<String, Object>>> getBundles() {
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("message", "Bundles retrieved");
        body.put("data", new ArrayList<>());

        return Mono.just(ResponseEntity.ok(body));
    }

    @GetMapping("/api/v1/admin/ai/index-jobs")
    public Mono<ResponseEntity<Map<String, Object>>> getIndexJobs() {
        Map<String, Object> pageData = new HashMap<>();
        pageData.put("content", new ArrayList<>());
        pageData.put("totalElements", 0);
        pageData.put("totalPages", 1);
        pageData.put("number", 0);

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("message", "Index jobs retrieved");
        body.put("data", pageData);

        return Mono.just(ResponseEntity.ok(body));
    }

    @PostMapping("/api/v1/admin/ai/index-jobs")
    public Mono<ResponseEntity<Map<String, Object>>> createIndexJob() {
        Map<String, Object> jobData = new HashMap<>();
        jobData.put("id", "mock-job-id");
        jobData.put("status", "COMPLETED");

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("message", "Index job created successfully");
        body.put("data", jobData);

        return Mono.just(ResponseEntity.ok(body));
    }
}
