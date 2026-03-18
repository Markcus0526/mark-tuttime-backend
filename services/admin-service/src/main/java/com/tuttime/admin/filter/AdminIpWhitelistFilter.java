package com.tuttime.admin.filter;

import com.tuttime.admin.config.IpWhitelistConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Admin IP Whitelist Filter: filters requests based on IP whitelist if enabled. Only applies to admin endpoints. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminIpWhitelistFilter extends OncePerRequestFilter {

    private final IpWhitelistConfig ipWhitelistConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Only apply to admin endpoints
        String path = request.getRequestURI();
        if (!path.startsWith("/api/admin/")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!ipWhitelistConfig.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String ipAddress = extractIpAddress(request);

        if (!ipWhitelistConfig.isIpAllowed(ipAddress)) {
            log.warn("IP address not whitelisted: ip={}, path={}", ipAddress, path);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Access denied. IP address not whitelisted.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** Extracts client IP address. */
    private String extractIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

