package com.stylemind.gateway.error;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
@Slf4j
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    private final GatewayErrorResponseWriter errorResponseWriter;

    public GatewayExceptionHandler(GatewayErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        if (isTimeout(ex)) {
            log.warn("Gateway downstream timeout: requestId={}", exchange.getRequest().getHeaders().getFirst("X-Request-Id"));
            return errorResponseWriter.write(exchange, HttpStatus.GATEWAY_TIMEOUT,
                    "DOWNSTREAM_TIMEOUT", "Downstream service timed out");
        }

        if (isConnectionFailure(ex)) {
            log.warn("Gateway downstream unavailable: requestId={}", exchange.getRequest().getHeaders().getFirst("X-Request-Id"));
            return errorResponseWriter.write(exchange, HttpStatus.BAD_GATEWAY,
                    "DOWNSTREAM_UNAVAILABLE", "Downstream service is unavailable");
        }

        if (ex instanceof ResponseStatusException statusException) {
            HttpStatus status = HttpStatus.valueOf(statusException.getStatusCode().value());
            return errorResponseWriter.write(exchange, status,
                    "GATEWAY_ERROR", "Gateway request failed");
        }

        log.error("Unhandled gateway error: requestId={}", exchange.getRequest().getHeaders().getFirst("X-Request-Id"));
        return errorResponseWriter.write(exchange, HttpStatus.INTERNAL_SERVER_ERROR,
                "GATEWAY_ERROR", "Gateway request failed");
    }

    private boolean isTimeout(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof TimeoutException || current.getClass().getName().contains("Timeout")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isConnectionFailure(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ConnectException
                    || current.getClass().getName().contains("ConnectException")
                    || current.getClass().getName().contains("UnknownHostException")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
