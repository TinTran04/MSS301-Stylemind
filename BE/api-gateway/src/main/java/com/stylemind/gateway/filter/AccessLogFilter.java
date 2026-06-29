package com.stylemind.gateway.filter;

import com.stylemind.gateway.support.GatewayExchangeAttributes;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AccessLogFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startedAtNanos = System.nanoTime();
        return chain.filter(exchange)
                .doFinally(signalType -> logAccess(exchange, startedAtNanos));
    }

    private void logAccess(ServerWebExchange exchange, long startedAtNanos) {
        long durationMs = (System.nanoTime() - startedAtNanos) / 1_000_000;
        String requestId = attr(exchange, GatewayExchangeAttributes.REQUEST_ID).orElse("");
        String userId = attr(exchange, GatewayExchangeAttributes.USER_ID).orElse("anonymous");
        String routeId = Optional.ofNullable(exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR))
                .filter(Route.class::isInstance)
                .map(Route.class::cast)
                .map(Route::getId)
                .orElse("unmatched");
        HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
        int status = statusCode == null ? 0 : statusCode.value();

        log.info("requestId={} method={} route={} status={} durationMs={} userId={}",
                requestId,
                exchange.getRequest().getMethod(),
                routeId,
                status,
                durationMs,
                userId);
    }

    private Optional<String> attr(ServerWebExchange exchange, String name) {
        Object value = exchange.getAttribute(name);
        return value instanceof String text && !text.isBlank() ? Optional.of(text) : Optional.empty();
    }

    @Override
    public int getOrder() {
        return -400;
    }
}
