package com.tuttime.admin.filter;

import com.tuttime.admin.service.AdminAuthService;
import com.tuttime.admin.service.AdminTokenBlacklistService;
import com.tuttime.admin.service.AdminTokenWhitelistService;
import com.tuttime.admin.util.AdminJwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/** Admin JWT Authentication Filter: validates admin JWT tokens with comprehensive security checks (token must be whitelisted, not blacklisted, ISS/AUD validation, signature and expiration validation). */
@Slf4j
public class AdminJwtAuthenticationFilter extends OncePerRequestFilter {

    private final AdminJwtTokenUtil jwtTokenUtil;
    private final AdminAuthService authService;
    private final AdminTokenWhitelistService tokenWhitelistService;
    private final AdminTokenBlacklistService tokenBlacklistService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    public AdminJwtAuthenticationFilter(
            AdminJwtTokenUtil jwtTokenUtil,
            AdminAuthService authService,
            AdminTokenWhitelistService tokenWhitelistService,
            AdminTokenBlacklistService tokenBlacklistService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.authService = authService;
        this.tokenWhitelistService = tokenWhitelistService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestUri = request.getRequestURI();
        
        boolean isHealthCheck = requestUri != null && (
            requestUri.contains("/actuator/health") || 
            requestUri.contains("/health")
        );
        
        String token = extractTokenFromRequest(request);
        
        if (token == null) {
            if (!isHealthCheck) {
                log.debug("No JWT token found in request: {}", requestUri);
            }
        } else {
            log.debug("JWT token found in request: {}, length: {}", request.getRequestURI(), token.length());
            try {
                String validationResult = validateTokenWithDetails(token);
                if ("SUCCESS".equals(validationResult)) {
                    setAuthenticationContext(token, request);
                    log.debug("Admin authentication successful for: {}", request.getRequestURI());
                } else {
                    log.warn("Admin token validation failed for: {} - Reason: {}", request.getRequestURI(), validationResult);
                }
            } catch (Exception e) {
                String stackTraceSummary = com.tuttime.common.exception.StackTraceUtil.getStackTraceSummary(e, 3);
                log.error("Cannot set admin authentication for {}: {}, stackTrace={}", request.getRequestURI(), e.getMessage(), stackTraceSummary);
            }
        }
        
        filterChain.doFilter(request, response);
    }

    /** Extracts JWT token from Authorization header. */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }

    /**
     * Validate admin token with comprehensive security checks
     * @return "SUCCESS" if valid, or a descriptive error message if invalid
     */
    private String validateTokenWithDetails(String token) {
        try {
            // Step 1: Extract JTI
            String jti = jwtTokenUtil.getJtiFromToken(token);
            if (jti == null) {
                return "Missing JTI claim";
            }
            log.debug("Extracted JTI from token: {}", jti);
            
            // Step 2: Check whitelist (MANDATORY - token must be whitelisted)
            boolean isWhitelisted = tokenWhitelistService.isTokenWhitelisted(jti);
            if (!isWhitelisted) {
                return "Token not whitelisted (jti=" + jti + ") - Token may not have been added during login, or Redis connection failed";
            }
            log.debug("Token is whitelisted: jti={}", jti);
            
            // Step 3: Check blacklist
            if (tokenBlacklistService.isTokenBlacklisted(jti)) {
                return "Token blacklisted (jti=" + jti + ")";
            }
            
            // Step 4: Validate issuer (ISS) - MANDATORY
            if (!jwtTokenUtil.validateIssuer(token)) {
                return "ISS validation failed - issuer mismatch";
            }
            
            // Step 5: Validate audience (AUD) - MANDATORY
            if (!jwtTokenUtil.validateAudience(token)) {
                return "AUD validation failed - audience mismatch";
            }
            
            // Step 6: Validate signature and expiration
            boolean signatureValid = jwtTokenUtil.validateTokenSignature(token);
            if (!signatureValid) {
                return "Signature or expiration validation failed - token may be expired or signature invalid";
            }
            
            log.debug("Token validation successful: jti={}", jti);
            return "SUCCESS";
            
        } catch (Exception e) {
            String stackTraceSummary = com.tuttime.common.exception.StackTraceUtil.getStackTraceSummary(e, 3);
            log.error("Admin token validation failed with exception: {}, stackTrace={}", e.getMessage(), stackTraceSummary);
            return "Exception during validation: " + e.getMessage();
        }
    }

    /**
     * Validate admin token with comprehensive security checks
     * @deprecated Use validateTokenWithDetails for better error reporting
     */
    @Deprecated
    private boolean validateToken(String token) {
        return "SUCCESS".equals(validateTokenWithDetails(token));
    }

    /**
     * Set authentication context for Spring Security
     */
    private void setAuthenticationContext(String token, HttpServletRequest request) {
        try {
            Long adminId = jwtTokenUtil.getAdminIdFromToken(token);
            String username = jwtTokenUtil.getUsernameFromToken(token);
            String role = jwtTokenUtil.getRoleFromToken(token);
            
            if (adminId != null && username != null && role != null) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        adminId,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                );
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.error("Failed to set admin authentication context: {}", e.getMessage());
        }
    }
}

