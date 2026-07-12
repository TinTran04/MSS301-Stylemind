package com.stylemind.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Component
@Slf4j
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Autowired
    public RateLimitFilter(@Autowired(required = false) ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private record Rule(String bucket, int maxRequests, Duration window, boolean keyByUser, String errorCode, String message) {}

    // SEC-09: login & forgot-password are unauthenticated, so they're keyed by
    // client IP - there's no X-User-Id yet at that point in the flow.
    private static final Map<String, Rule> RULES = Map.of(
            "/api/v1/ai-stylist/chat", new Rule("ai-chat", 5, Duration.ofMinutes(1), true,
                    "AI_RATE_LIMIT_EXCEEDED", "Vượt quá giới hạn 5 yêu cầu/phút cho AI Stylist"),
            "/api/v1/auth/login", new Rule("login", 10, Duration.ofMinutes(1), false,
                    "AUTH_RATE_LIMIT_EXCEEDED", "Vượt quá giới hạn thử đăng nhập, vui lòng thử lại sau"),
            "/api/v1/auth/forgot-password", new Rule("forgot-password", 5, Duration.ofMinutes(5), false,
                    "AUTH_RATE_LIMIT_EXCEEDED", "Vượt quá giới hạn yêu cầu quên mật khẩu, vui lòng thử lại sau")
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // If Redis is not available, skip rate limiting (fail open)
        if (redisTemplate == null) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getPath().value();
        Rule rule = RULES.get(path);
        if (rule == null) {
            return chain.filter(exchange);
        }

        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        String ip = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";

        String identity = rule.keyByUser() && userId != null ? userId : "ip:" + ip;
        String key = "ratelimit:" + rule.bucket() + ":" + identity;

        return redisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {
                    if (count == 1) {
                        redisTemplate.expire(key, rule.window()).subscribe();
                    }

                    if (count > rule.maxRequests()) {
                        log.warn("Rate limit exceeded for {}: identity={}", rule.bucket(), identity);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                        String body = String.format(
                                "{\"success\":false,\"errorCode\":\"%s\",\"message\":\"%s\"}",
                                rule.errorCode(), rule.message());
                        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes())));
                    }

                    return chain.filter(exchange);
                })
                .onErrorResume(ex -> {
                    log.error("Rate limit check failed", ex);
                    return chain.filter(exchange); // Fail open
                });
    }

    @Override
    public int getOrder() {
        return -50; // After JWT filter
    }
}
