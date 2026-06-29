package com.stylemind.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stylemind.gateway.config.GatewayRateLimitProperties;
import com.stylemind.gateway.error.GatewayErrorResponseWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final int MAX_BODY_BYTES_FOR_IDENTIFIER = 8192;

    private final ReactiveStringRedisTemplate redisTemplate;
    private final GatewayRateLimitProperties properties;
    private final GatewayErrorResponseWriter errorResponseWriter;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        RateLimitPlan plan = planFor(exchange.getRequest());
        if (plan.rules().isEmpty()) {
            return chain.filter(exchange);
        }

        if (plan.needsIdentifierFromBody()) {
            return withCachedBody(exchange, chain, plan);
        }
        return applyRateLimits(exchange, chain, plan, null);
    }

    private Mono<Void> withCachedBody(ServerWebExchange exchange, GatewayFilterChain chain, RateLimitPlan plan) {
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .defaultIfEmpty(exchange.getResponse().bufferFactory().wrap(new byte[0]))
                .flatMap(dataBuffer -> {
                    byte[] body = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(body);
                    DataBufferUtils.release(dataBuffer);

                    if (body.length > MAX_BODY_BYTES_FOR_IDENTIFIER) {
                        return applyRateLimits(exchange, chain, plan, null);
                    }

                    ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                        @Override
                        public Flux<org.springframework.core.io.buffer.DataBuffer> getBody() {
                            return Flux.defer(() -> Flux.just(exchange.getResponse().bufferFactory().wrap(body)));
                        }
                    };
                    ServerWebExchange decoratedExchange = exchange.mutate().request(decoratedRequest).build();
                    return applyRateLimits(decoratedExchange, chain, plan,
                            extractIdentifierHash(body, plan.identifierFields()));
                });
    }

    private Mono<Void> applyRateLimits(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            RateLimitPlan plan,
            String identifierHash) {
        List<RateLimitRule> rules = new ArrayList<>(plan.rules());
        if (StringUtils.hasText(identifierHash)) {
            rules.add(plan.identifierRule(identifierHash));
        }

        return Flux.fromIterable(rules)
                .concatMap(this::isLimited)
                .any(Boolean::booleanValue)
                .flatMap(limited -> limited
                        ? errorResponseWriter.write(exchange, HttpStatus.TOO_MANY_REQUESTS,
                                "RATE_LIMIT_EXCEEDED", "Too many requests")
                        : chain.filter(exchange))
                .onErrorResume(ex -> handleRedisFailure(exchange, chain, ex));
    }

    private Mono<Boolean> isLimited(RateLimitRule rule) {
        return redisTemplate.opsForValue()
                .increment(rule.key())
                .timeout(properties.getRedisTimeout())
                .flatMap(count -> {
                    Mono<Boolean> expiry = count == 1
                            ? redisTemplate.expire(rule.key(), rule.window()).timeout(properties.getRedisTimeout())
                            : Mono.just(true);
                    return expiry.thenReturn(count > rule.limit());
                });
    }

    private Mono<Void> handleRedisFailure(ServerWebExchange exchange, GatewayFilterChain chain, Throwable ex) {
        if (properties.isFailOpen()) {
            log.warn("Rate limit unavailable; failing open: requestId={}",
                    exchange.getRequest().getHeaders().getFirst("X-Request-Id"));
            return chain.filter(exchange);
        }
        log.warn("Rate limit unavailable; failing closed: requestId={}",
                exchange.getRequest().getHeaders().getFirst("X-Request-Id"));
        return errorResponseWriter.write(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                "RATE_LIMIT_UNAVAILABLE", "Rate limit service is unavailable");
    }

    private RateLimitPlan planFor(ServerHttpRequest request) {
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();
        String ip = clientIp(request);

        if (HttpMethod.POST.equals(method) && "/api/auth/login".equals(path)) {
            return RateLimitPlan.withOptionalIdentifier(List.of(rule("login:ip:" + ip, properties.getLoginIp())),
                    properties.getLoginIdentifier(), "login:identifier:");
        }
        if (HttpMethod.POST.equals(method) && "/api/auth/register".equals(path)) {
            return RateLimitPlan.noIdentifier(List.of(rule("register:ip:" + ip, properties.getRegisterIp())));
        }
        if (HttpMethod.POST.equals(method) && "/api/auth/forgot-password".equals(path)) {
            return RateLimitPlan.withOptionalIdentifier(
                    List.of(rule("forgot-password:ip:" + ip, properties.getForgotPasswordIp())),
                    properties.getForgotPasswordIdentifier(),
                    "forgot-password:identifier:",
                    List.of("email", "identifier"));
        }
        if (HttpMethod.POST.equals(method) && "/api/auth/reset-password".equals(path)) {
            return RateLimitPlan.withOptionalIdentifier(
                    List.of(rule("reset-password:ip:" + ip, properties.getResetPasswordIp())),
                    properties.getResetPasswordToken(),
                    "reset-password:token:",
                    List.of("token"));
        }
        if (HttpMethod.POST.equals(method) && "/api/auth/refresh".equals(path)) {
            return RateLimitPlan.noIdentifier(List.of(rule("refresh:ip:" + ip, properties.getRefreshIp())));
        }
        if (HttpMethod.POST.equals(method) && "/api/ai-stylist/chat".equals(path)) {
            String userId = request.getHeaders().getFirst("X-User-Id");
            String subject = StringUtils.hasText(userId) ? "user:" + userId : "ip:" + ip;
            return RateLimitPlan.noIdentifier(List.of(rule("ai-chat:" + subject, properties.getAiChat())));
        }
        return RateLimitPlan.noIdentifier(List.of());
    }

    private RateLimitRule rule(String suffix, GatewayRateLimitProperties.Policy policy) {
        return new RateLimitRule("ratelimit:gateway:" + suffix, policy.getLimit(), policy.getWindow());
    }

    private String clientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        return remoteAddress == null || remoteAddress.getAddress() == null
                ? "unknown"
                : remoteAddress.getAddress().getHostAddress();
    }

    private String extractIdentifierHash(byte[] body, List<String> fieldNames) {
        if (body.length == 0) {
            return null;
        }
        try {
            JsonNode json = objectMapper.readTree(body);
            String identifier = null;
            for (String fieldName : fieldNames) {
                identifier = firstText(json, fieldName);
                if (StringUtils.hasText(identifier)) {
                    break;
                }
            }
            if (!StringUtils.hasText(identifier)) {
                return null;
            }
            return sha256(identifier.trim().toLowerCase(Locale.ROOT));
        } catch (Exception ex) {
            return null;
        }
    }

    private String firstText(JsonNode json, String fieldName) {
        JsonNode value = json.get(fieldName);
        return value == null || !value.isTextual() ? null : value.asText();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required", ex);
        }
    }

    @Override
    public int getOrder() {
        return -50;
    }

    private record RateLimitRule(String key, int limit, Duration window) {
    }

    private record RateLimitPlan(
            List<RateLimitRule> rules,
            boolean needsIdentifierFromBody,
            GatewayRateLimitProperties.Policy identifierPolicy,
            String identifierPrefix,
            List<String> identifierFields) {

        static RateLimitPlan noIdentifier(List<RateLimitRule> rules) {
            return new RateLimitPlan(rules, false, null, null, List.of());
        }

        static RateLimitPlan withOptionalIdentifier(
                List<RateLimitRule> rules,
                GatewayRateLimitProperties.Policy identifierPolicy,
                String identifierPrefix) {
            return withOptionalIdentifier(rules, identifierPolicy, identifierPrefix, List.of("email", "identifier"));
        }

        static RateLimitPlan withOptionalIdentifier(
                List<RateLimitRule> rules,
                GatewayRateLimitProperties.Policy identifierPolicy,
                String identifierPrefix,
                List<String> identifierFields) {
            return new RateLimitPlan(rules, true, identifierPolicy, identifierPrefix, identifierFields);
        }

        RateLimitRule identifierRule(String identifierHash) {
            return new RateLimitRule(
                    "ratelimit:gateway:" + identifierPrefix + identifierHash,
                    identifierPolicy.getLimit(),
                    identifierPolicy.getWindow());
        }
    }
}
