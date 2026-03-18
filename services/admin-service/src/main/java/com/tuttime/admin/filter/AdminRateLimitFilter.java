package com.tuttime.admin.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/** Admin Rate Limit Filter: enforces rate limiting on admin endpoints to prevent brute-force attacks and abuse. Uses Bucket4j with Redis for distributed rate limiting. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminRateLimitFilter extends OncePerRequestFilter {

    @Autowired(required = false)
    private LettuceBasedProxyManager<byte[]> proxyManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        if (path.equals("/api/admin/auth/login")) {
            String ipAddress = getClientIpAddress(request);
            if (proxyManager != null) {
                Bucket bucket = createBucketForIp(proxyManager, ipAddress);
                if (bucket != null && !bucket.tryConsume(1)) {
                    log.warn("Rate limit exceeded for admin login: ip={}", ipAddress);
                    response.setStatus(429); // Too Many Requests
                    response.setContentType("application/json");
                    try {
                        response.getWriter().write("{\"error\":\"Too many login attempts. Please try again later.\"}");
                    } catch (IOException ioException) {
                        String stackTraceSummary = com.tuttime.common.exception.StackTraceUtil.getStackTraceSummary(ioException, 2);
                        log.error("Error writing rate limit response, stackTrace={}", stackTraceSummary);
                    }
                    return;
                }
            }
        }
        
        if (path.startsWith("/api/admin/") && !path.equals("/api/admin/auth/login")) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && proxyManager != null) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof Long) {
                    Long adminId = (Long) principal;
                    Bucket bucket = createBucketForAdmin(proxyManager, adminId);
                    if (bucket != null && !bucket.tryConsume(1)) {
                        log.warn("Rate limit exceeded for admin endpoint: adminId={}, path={}", adminId, path);
                        response.setStatus(429); // Too Many Requests
                        response.setContentType("application/json");
                        try {
                            response.getWriter().write("{\"error\":\"Rate limit exceeded. Please slow down.\"}");
                        } catch (IOException ioException) {
                            log.error("Error writing rate limit response", ioException);
                        }
                        return;
                    }
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * Create bucket for IP-based rate limiting
     */
    private Bucket createBucketForIp(LettuceBasedProxyManager<byte[]> proxyManager, String ipAddress) {
        if (proxyManager == null) {
            return null; // Rate limiting disabled if Redis unavailable
        }
        try {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(5)
                    .refillIntervally(5, Duration.ofMinutes(15))
                    .build();
            BucketConfiguration bucketConfig = BucketConfiguration.builder()
                    .addLimit(limit)
                    .build();
            return proxyManager.builder()
                    .build(("admin:rate-limit:login:" + ipAddress).getBytes(), () -> bucketConfig);
        } catch (Exception e) {
            log.error("Error creating rate limit bucket for IP: {}", ipAddress, e);
            return null;
        }
    }

    /**
     * Create bucket for admin-based rate limiting
     */
    private Bucket createBucketForAdmin(LettuceBasedProxyManager<byte[]> proxyManager, Long adminId) {
        if (proxyManager == null) {
            return null; // Rate limiting disabled if Redis unavailable
        }
        try {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(100)
                    .refillIntervally(100, Duration.ofMinutes(1))
                    .build();
            BucketConfiguration bucketConfig = BucketConfiguration.builder()
                    .addLimit(limit)
                    .build();
            return proxyManager.builder()
                    .build(("admin:rate-limit:authenticated:" + adminId).getBytes(), () -> bucketConfig);
        } catch (Exception e) {
            String stackTraceSummary = com.tuttime.common.exception.StackTraceUtil.getStackTraceSummary(e, 2);
            log.error("Error creating rate limit bucket for admin: {}, stackTrace={}", adminId, stackTraceSummary);
            return null;
        }
    }

    /**
     * Extract client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}

