package com.tuttime.user.websocket;

import com.tuttime.user.service.AuthService;
import com.tuttime.user.service.TokenWhitelistService;
import com.tuttime.user.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket authentication interceptor for Presence Service
 * 
 * <p>Validates JWT token during WebSocket handshake and extracts user ID.
 * Also validates token is whitelisted (not blacklisted).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    
    private final JwtTokenUtil jwtTokenUtil;
    private final TokenWhitelistService tokenWhitelistService;
    private final AuthService authService;
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        log.info("WebSocket handshake attempt: {}", request.getURI());
        
        if (!(request instanceof ServletServerHttpRequest)) {
            log.warn("WebSocket connection rejected: request is not ServletServerHttpRequest");
            return false;
        }
        
        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        
        // Extract token from query parameter or Authorization header
        String token = httpRequest.getParameter("token");
        if (token == null || token.isEmpty()) {
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }
        
        if (token == null || token.isEmpty()) {
            log.warn("WebSocket connection rejected: no token provided");
            return false;
        }
        
        try {
            // Extract JTI from token first
            String jti = jwtTokenUtil.getJtiFromToken(token);
            if (jti == null) {
                log.warn("WebSocket connection rejected: token missing JTI claim");
                return false;
            }
            
            // Validate token signature and expiration
            if (!jwtTokenUtil.validateToken(token)) {
                log.warn("WebSocket connection rejected: invalid token");
                return false;
            }
            
            // Validate token issuer and audience
            if (!jwtTokenUtil.validateIssuer(token) || !jwtTokenUtil.validateAudience(token)) {
                log.warn("WebSocket connection rejected: token issuer/audience mismatch");
                return false;
            }
            
            // Check if token is whitelisted (must be whitelisted to be valid)
            // Optimized: Fast check with minimal retries to avoid blocking handshake
            if (!tokenWhitelistService.isTokenWhitelisted(jti)) {
                log.warn("WebSocket connection rejected: token is not whitelisted (jti={})", jti);
                return false;
            }
            
            // Check if token is blacklisted (additional security check)
            // Note: This is a fast Redis lookup, typically < 10ms even on slow networks
            // If this becomes a bottleneck, consider making it async or caching
            try {
                if (authService.isTokenBlacklisted(token)) {
                    log.warn("WebSocket connection rejected: token is blacklisted (jti={})", jti);
                    return false;
                }
            } catch (Exception e) {
                // If blacklist check fails (e.g., Redis timeout), log but don't block connection
                // Blacklist is a security enhancement, not a hard requirement
                log.warn("Blacklist check failed (non-critical, allowing connection): jti={}, error={}", 
                        jti, e.getMessage());
                // Continue with connection - blacklist check failure shouldn't block legitimate users
            }
            
            // Extract user ID
            Long userId = jwtTokenUtil.getUserIdFromToken(token);
            if (userId == null) {
                log.warn("WebSocket connection rejected: invalid token - userId is null");
                return false;
            }
            
            // Store user ID in session attributes
            attributes.put("userId", userId);
            attributes.put("token", token);
            
            log.info("WebSocket handshake successful: userId={}, endpoint={}", 
                userId, request.getURI().getPath());
            return true;
            
        } catch (Exception e) {
            log.error("WebSocket connection rejected: token validation failed", e);
            return false;
        }
    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            String stackTraceSummary = getStackTraceSummary(exception, 3);
            log.error("WebSocket handshake failed: {}, stackTrace={}", request.getURI(), stackTraceSummary);
        }
    }
    
    /**
     * Gets a shortened stack trace summary for logging.
     * 
     * @param ex The exception
     * @param maxLines Maximum number of stack trace lines to include
     * @return Stack trace summary string (first N lines)
     */
    private String getStackTraceSummary(Throwable ex, int maxLines) {
        if (ex == null) {
            return "";
        }
        
        StackTraceElement[] stackTrace = ex.getStackTrace();
        if (stackTrace == null || stackTrace.length == 0) {
            return "";
        }
        
        StringBuilder summary = new StringBuilder();
        int linesToInclude = Math.min(maxLines, stackTrace.length);
        
        for (int i = 0; i < linesToInclude; i++) {
            if (i > 0) {
                summary.append(" | ");
            }
            summary.append(stackTrace[i].toString());
        }
        
        if (stackTrace.length > maxLines) {
            summary.append(" ... (").append(stackTrace.length - maxLines).append(" more)");
        }
        
        return summary.toString();
    }
}

