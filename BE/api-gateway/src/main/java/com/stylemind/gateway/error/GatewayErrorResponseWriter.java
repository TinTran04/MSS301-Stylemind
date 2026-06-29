package com.stylemind.gateway.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stylemind.gateway.support.GatewayExchangeAttributes;
import com.stylemind.gateway.support.GatewayHeaders;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public GatewayErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        String requestId = resolveRequestId(exchange);
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.empty();
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(GatewayHeaders.REQUEST_ID, requestId);

        Map<String, Object> body = Map.of(
                "success", false,
                "error", Map.of(
                        "code", code,
                        "message", message),
                "requestId", requestId);

        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException ex) {
            payload = fallbackBody(code, message, requestId);
        }

        return response.writeWith(Mono.just(response.bufferFactory().wrap(payload)));
    }

    private String resolveRequestId(ServerWebExchange exchange) {
        Object attribute = exchange.getAttribute(GatewayExchangeAttributes.REQUEST_ID);
        if (attribute instanceof String requestId && !requestId.isBlank()) {
            return requestId;
        }
        String requestHeader = exchange.getRequest().getHeaders().getFirst(GatewayHeaders.REQUEST_ID);
        return requestHeader == null || requestHeader.isBlank() ? "" : requestHeader;
    }

    private byte[] fallbackBody(String code, String message, String requestId) {
        String json = String.format(
                "{\"success\":false,\"error\":{\"code\":\"%s\",\"message\":\"%s\"},\"requestId\":\"%s\"}",
                code,
                message,
                requestId);
        return json.getBytes(StandardCharsets.UTF_8);
    }
}
