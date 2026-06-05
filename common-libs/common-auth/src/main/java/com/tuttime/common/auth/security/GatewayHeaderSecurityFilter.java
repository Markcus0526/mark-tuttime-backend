package com.tuttime.common.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class GatewayHeaderSecurityFilter extends OncePerRequestFilter {

    // Reads the shared cluster secret key from your properties or environment variables
    @Value("${cluster.internal.secret:fallback-dev-secret-key-123}")
    private String internalClusterSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. CRITICAL: Validate that the request came directly through your API Gateway
        String gatewayToken = request.getHeader("X-Internal-Gateway-Token");

        if (gatewayToken == null || !gatewayToken.equals(internalClusterSecret)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Access Denied: Direct internal access unauthorized.\"}");
            return;
        }

        String userId = request.getHeader("X-User-Id");
        String rolesHeader = request.getHeader("X-User-Roles");

        // 2. Spring Security context setup
        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            List<SimpleGrantedAuthority> authorities = List.of();
            if (rolesHeader != null && !rolesHeader.isBlank()) {
                authorities = Arrays.stream(rolesHeader.split(","))
                        .map(SimpleGrantedAuthority::new)
                        .toList();
            }
            var authToken = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // 3. Populate custom UserContext
        if (userId != null && !userId.isBlank()) {
            UserContext.setUserId(userId);
        }
        if (rolesHeader != null && !rolesHeader.isBlank()) {
            UserContext.setRoles(rolesHeader);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 4. Prevent memory leaks
            UserContext.clear();
        }
    }
}
