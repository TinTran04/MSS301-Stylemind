package com.stylemind.gateway.filter;

import com.stylemind.gateway.support.GatewayExchangeAttributes;
import com.stylemind.gateway.support.GatewayHeaders;
import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RequestIdFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = UUID.randomUUID().toString();
        exchange.getAttributes().put(GatewayExchangeAttributes.REQUEST_ID, requestId);

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.remove(GatewayHeaders.REQUEST_ID))
                .header(GatewayHeaders.REQUEST_ID, requestId)
                .build();

        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(GatewayHeaders.REQUEST_ID, requestId);
            return Mono.empty();
        });

        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return -300;
    }
}
