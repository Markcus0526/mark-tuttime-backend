package com.tuttime.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Response Logging Filter: logs outgoing responses including status code and response time (no body logging).
 */
@Slf4j
@Component
public class ResponseLoggingFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = (String) exchange.getAttributes().get(CORRELATION_ID_HEADER);
        Instant startTime = (Instant) exchange.getAttributes().get("requestStartTime");

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            if (log.isDebugEnabled()) {
                logResponse(exchange, correlationId, startTime);
            }
        }));
    }

    private void logResponse(ServerWebExchange exchange, String correlationId, Instant startTime) {
        ServerHttpResponse response = exchange.getResponse();

        long responseTime = 0;
        if (startTime != null) {
            responseTime = Duration.between(startTime, Instant.now()).toMillis();
        }

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Outgoing Response: ");
        logMessage.append("correlationId=").append(correlationId != null ? correlationId : "N/A");
        if (response.getStatusCode() != null) {
            logMessage.append(", status=").append(response.getStatusCode().value());
        } else {
            logMessage.append(", status=N/A");
        }
        logMessage.append(", responseTime=").append(responseTime).append("ms");

        // Log response size if available
        if (response.getHeaders().getContentLength() > 0) {
            logMessage.append(", size=").append(response.getHeaders().getContentLength()).append(" bytes");
        }

        log.debug(logMessage.toString());
    }

    @Override
    public int getOrder() {
        // Execute last in the filter chain
        return Ordered.LOWEST_PRECEDENCE;
    }
}

