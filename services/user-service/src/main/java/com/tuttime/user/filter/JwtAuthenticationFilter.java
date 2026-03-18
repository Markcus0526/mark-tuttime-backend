package com.tuttime.user.filter;

import com.tuttime.user.service.AuthService;
import com.tuttime.user.service.TokenWhitelistService;
import com.tuttime.user.util.JwtTokenUtil;
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

/**
 * JWT Authentication Filter
 * 
 * <p>This filter intercepts HTTP requests and validates JWT tokens from the Authorization header.
 * If a valid token is found, it sets the Spring Security authentication context.
 * 
 * <p>The filter performs comprehensive token validation:
 * <ul>
 *   <li>Extracts the Bearer token from the Authorization header</li>
 *   <li>Extracts JTI (JWT ID) from token</li>
 *   <li>Checks if token is whitelisted (must be whitelisted to be valid)</li>
 *   <li>Checks if token is blacklisted (revoked tokens)</li>
 *   <li>Validates token issuer (ISS claim)</li>
 *   <li>Validates token audience (AUD claim)</li>
 *   <li>Validates token signature and expiration</li>
 *   <li>Sets the authentication context for Spring Security</li>
 * </ul>
 * 
 * <p><b>Security Notes:</b>
 * <ul>
 *   <li>Tokens must be whitelisted to be accepted (prevents revoked tokens)</li>
 *   <li>ISS and AUD validation prevents accepting tokens from other systems</li>
 *   <li>Blacklist check provides additional security layer</li>
 * </ul>
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final AuthService authService;
    private final TokenWhitelistService tokenWhitelistService;

    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil, AuthService authService, TokenWhitelistService tokenWhitelistService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.authService = authService;
        this.tokenWhitelistService = tokenWhitelistService;
    }

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestUri = request.getRequestURI();
        
        // Skip logging for health check endpoints to reduce noise
        boolean isHealthCheck = requestUri != null && (
            requestUri.contains("/actuator/health") || 
            requestUri.contains("/health")
        );
        
        try {
            String token = extractTokenFromRequest(request);
            
            if (token != null && validateToken(token, isHealthCheck)) {
                setAuthenticationContext(token, request, isHealthCheck);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
            // Continue filter chain - let Spring Security handle unauthorized requests
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }

    /**
     * Validate token with comprehensive security checks
     * 
     * <p>Validation order:
     * <ol>
     *   <li>Extract JTI from token</li>
     *   <li>Check whitelist (token must be whitelisted)</li>
     *   <li>Check blacklist (token must not be blacklisted)</li>
     *   <li>Validate issuer (ISS claim)</li>
     *   <li>Validate audience (AUD claim)</li>
     *   <li>Validate signature and expiration</li>
     * </ol>
     * 
     * @param token JWT token to validate
     * @param isHealthCheck whether this is a health check request (to suppress debug logs)
     * @return true if token passes all validation checks
     */
    private boolean validateToken(String token, boolean isHealthCheck) {
        try {
            // Step 1: Extract JTI from token
            String jti = jwtTokenUtil.getJtiFromToken(token);
            if (jti == null) {
                if (!isHealthCheck) {
                    log.debug("Token missing JTI claim");
                }
                return false;
            }
            
            // Step 2: Check whitelist first (token must be whitelisted)
            if (!tokenWhitelistService.isTokenWhitelisted(jti)) {
                if (!isHealthCheck) {
                    log.debug("Token is not whitelisted: jti={}", jti);
                }
                return false;
            }
            
            // Step 3: Check blacklist second (token must not be blacklisted)
            if (authService.isTokenBlacklisted(token)) {
                if (!isHealthCheck) {
                    log.debug("Token is blacklisted: jti={}", jti);
                }
                return false;
            }
            
            // Step 4: Validate issuer (ISS claim)
            if (!jwtTokenUtil.validateIssuer(token)) {
                if (!isHealthCheck) {
                    log.debug("Token issuer validation failed");
                }
                return false;
            }
            
            // Step 5: Validate audience (AUD claim)
            if (!jwtTokenUtil.validateAudience(token)) {
                if (!isHealthCheck) {
                    log.debug("Token audience validation failed");
                }
                return false;
            }
            
            // Step 6: Validate token signature and expiration
            if (!jwtTokenUtil.validateToken(token)) {
                if (!isHealthCheck) {
                    log.debug("Token signature or expiration validation failed");
                }
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.warn("Token validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Set Spring Security authentication context from JWT token
     */
    private void setAuthenticationContext(String token, HttpServletRequest request, boolean isHealthCheck) {
        try {
            Long userId = jwtTokenUtil.getUserIdFromToken(token);
            String email = jwtTokenUtil.getEmailFromToken(token);
            
            if (userId != null && email != null) {
                // Create authentication token
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userId, // principal (user ID)
                        null,   // credentials (not needed)
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Set authentication in Spring Security context
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                if (!isHealthCheck) {
                    log.debug("Authentication set for user: {} ({})", userId, email);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to set authentication context: {}", e.getMessage());
        }
    }
}

