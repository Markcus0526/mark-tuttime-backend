package com.tuttime.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Security Headers Filter: adds security headers to all responses to protect against common web vulnerabilities. */
@Slf4j
@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Skip security headers for WebSocket endpoints
        String path = exchange.getRequest().getURI().getPath();
        if (path != null && path.startsWith("/ws/")) {
            return chain.filter(exchange);
        }
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            HttpHeaders headers = response.getHeaders();
            
            try {
                if (!headers.containsKey("X-Content-Type-Options")) {
                    headers.add("X-Content-Type-Options", "nosniff");
                }
                
                if (!headers.containsKey("X-Frame-Options")) {
                    headers.add("X-Frame-Options", "DENY");
                }
                
                if (!headers.containsKey("X-XSS-Protection")) {
                    headers.add("X-XSS-Protection", "1; mode=block");
                }
                
                // Strict-Transport-Security: Force HTTPS (only add if HTTPS is enabled)
                // Note: Only add in production with HTTPS enabled
                // headers.add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                
                // Content-Security-Policy: Restrict resource loading
                if (!headers.containsKey("Content-Security-Policy")) {
                    headers.add("Content-Security-Policy", "default-src 'self'");
                }
                
                // Referrer-Policy: Control referrer information
                if (!headers.containsKey("Referrer-Policy")) {
                    headers.add("Referrer-Policy", "strict-origin-when-cross-origin");
                }
                
                // Permissions-Policy: Restrict browser features
                if (!headers.containsKey("Permissions-Policy")) {
                    headers.add("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
                }
            } catch (UnsupportedOperationException e) {
                // Headers are read-only (e.g., after CORS processing), skip adding security headers
                log.debug("Cannot add security headers: headers are read-only for path: {}", path);
            }
        }));
    }

    @Override
    public int getOrder() {
        // Execute after response is ready but before it's sent
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}

