package com.tuttime.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/** Request Logging Filter: logs incoming requests for debugging and monitoring (masks sensitive headers). */
@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final List<String> SENSITIVE_HEADERS = List.of(
            "Authorization",
            "X-Api-Key",
            "Cookie"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = (String) exchange.getAttributes().get(CORRELATION_ID_HEADER);
        
        if (log.isDebugEnabled()) {
            logRequest(request, correlationId);
        }
        
        exchange.getAttributes().put("requestStartTime", Instant.now());
        
        return chain.filter(exchange);
    }

    private void logRequest(ServerHttpRequest request, String correlationId) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Incoming Request: ");
        logMessage.append("correlationId=").append(correlationId != null ? correlationId : "N/A");
        logMessage.append(", method=").append(request.getMethod());
        logMessage.append(", path=").append(request.getURI().getPath());
        logMessage.append(", query=").append(request.getURI().getQuery() != null ? request.getURI().getQuery() : "");
        logMessage.append(", remoteAddress=").append(getRemoteAddress(request));
        
        // Log headers (mask sensitive ones)
        HttpHeaders headers = request.getHeaders();
        if (!headers.isEmpty()) {
            logMessage.append(", headers={");
            String headerLog = headers.entrySet().stream()
                    .map(entry -> {
                        String headerName = entry.getKey();
                        if (SENSITIVE_HEADERS.contains(headerName)) {
                            return headerName + "=***MASKED***";
                        }
                        return headerName + "=" + String.join(", ", entry.getValue());
                    })
                    .collect(Collectors.joining(", "));
            logMessage.append(headerLog).append("}");
        }
        
        log.debug(logMessage.toString());
    }

    private String getRemoteAddress(ServerHttpRequest request) {
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    @Override
    public int getOrder() {
        // Execute after CorrelationIdFilter but before other filters
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}

