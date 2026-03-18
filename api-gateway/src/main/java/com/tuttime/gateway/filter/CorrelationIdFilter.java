package com.tuttime.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Correlation ID Filter: generates and propagates correlation ID for distributed tracing. */
@Slf4j
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header(CORRELATION_ID_HEADER, correlationId)
                        .header(REQUEST_ID_HEADER, correlationId)
                        .build())
                .build();
        
        modifiedExchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        modifiedExchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, correlationId);
        modifiedExchange.getAttributes().put(CORRELATION_ID_HEADER, correlationId);
        
        log.debug("Correlation ID generated/propagated: {}", correlationId);
        
        return chain.filter(modifiedExchange);
    }

    @Override
    public int getOrder() {
        // Execute early in the filter chain (before authentication, rate limiting, etc.)
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

