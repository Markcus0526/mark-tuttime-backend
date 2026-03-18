package com.aparttoken.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Correlation ID Filter for Servlet-based services.
 *
 * <p>This filter extracts or generates a correlation ID for each request
 * and adds it to the MDC (Mapped Diagnostic Context) for logging purposes.
 * It also ensures the correlation ID is included in response headers.
 *
 * <p>The filter:
 * <ul>
 *   <li>Extracts correlation ID from request header (X-Correlation-Id)</li>
 *   <li>Generates a new UUID if not present</li>
 *   <li>Adds correlation ID to MDC for logging</li>
 *   <li>Includes correlation ID in response headers</li>
 *   <li>Cleans up MDC after request processing</li>
 * </ul>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        // Generate correlation ID if not present
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Add correlation ID to MDC for logging
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

        // Add correlation ID to response headers
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Clean up MDC after request processing
            MDC.clear();
        }
    }
}

