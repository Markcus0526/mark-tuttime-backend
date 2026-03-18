package com.tuttime.admin.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Manual CORS filter: adds CORS headers manually to prevent conflicts with reverse proxies. */
@Slf4j
@Component
public class CorsHeaderFilter extends OncePerRequestFilter implements Ordered {

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:8080,https://boostx-admin.frontare.ink}")
    private String allowedOrigins;

    private List<String> allowedOriginList;
    private boolean initialized = false;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                     @NonNull HttpServletResponse response, 
                                     @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    allowedOriginList = Arrays.stream(allowedOrigins.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty() && !"*".equals(s))
                            .distinct()
                            .collect(Collectors.toList());
                    log.info("CORS filter initialized with allowed origins: {}", allowedOriginList);
                    initialized = true;
                }
            }
        }

        // Get origin from request
        String origin = request.getHeader("Origin");
        
        // Check if origin is allowed
        boolean isAllowedOrigin = origin != null && allowedOriginList.contains(origin);
        
        // Handle preflight OPTIONS request
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            if (isAllowedOrigin) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD");
                response.setHeader("Access-Control-Allow-Headers", "*");
                response.setHeader("Access-Control-Allow-Credentials", "true");
                response.setHeader("Access-Control-Max-Age", "3600");
                response.setStatus(HttpServletResponse.SC_OK);
                log.debug("CORS preflight request handled for origin: {}", origin);
                return; // Don't continue filter chain for preflight
            } else {
                log.warn("CORS preflight request from disallowed origin: {}", origin);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        // For regular requests, continue filter chain
        filterChain.doFilter(request, response);
        
        // After filter chain completes, set CORS headers explicitly
        // IMPORTANT: setHeader() REPLACES any existing header with the same name
        // This ensures no duplicates even if admin-service or another filter added them
        if (isAllowedOrigin) {
            // Set CORS headers (replaces any existing ones, preventing duplicates)
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Expose-Headers", 
                    "Authorization, Content-Type, X-Total-Count, X-Correlation-Id");
            log.debug("Set CORS headers for origin: {}", origin);
        }
    }

    @Override
    public int getOrder() {
        // Run LATE in the filter chain (low priority = high order value)
        // This ensures we set CORS headers AFTER any other filters/processors
        // and replace any existing CORS headers (preventing duplicates)
        return Ordered.LOWEST_PRECEDENCE - 100;
    }
}

