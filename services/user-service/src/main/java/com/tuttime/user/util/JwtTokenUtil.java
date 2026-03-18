package com.tuttime.user.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/** JWT Token Utility: handles JWT token generation and validation with JTI tracking, ISS/AUD claims, expiration and signature validation. */
@Slf4j
@Component
public class JwtTokenUtil {

    @Value("${spring.security.jwt.secret}")
    private String secret;

    @Value("${spring.security.jwt.expiration}")
    private Long expiration;
    
    @Value("${spring.security.jwt.issuer:apart-token-backend}")
    private String issuer;
    @Value("${spring.security.jwt.audience:apart-token-mobile-app}")
    private String audience;
    @Value("${sms.signup.token.expiration-minutes:15}")
    private int signupTokenExpirationMinutes;

    @PostConstruct
    public void init() {
        if (secret == null || secret.trim().isEmpty()) {
            secret = "apart-token-secret-key";
            log.warn("JWT secret was empty, using fallback. Please set JWT_SECRET environment variable!");
        }
        // Note: We don't pad the secret string here - padding is done in getSigningKey() using bytes
        // This ensures consistent key generation regardless of how the secret is stored
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            log.warn("JWT secret is too short ({} bytes), will be padded to 32 bytes at runtime. Please use a proper secret (min 32 bytes)!", keyBytes.length);
        }
        log.info("JWT secret initialized: length={} bytes ({} chars)", keyBytes.length, secret.length());
    }

    /** Get secret key from the configured secret string. */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // Secret should already be padded in init(), but add safety check
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
            for (int i = keyBytes.length; i < 32; i++) {
                paddedKey[i] = keyBytes[i % keyBytes.length];
            }
            keyBytes = paddedKey;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /** Generates unique JWT ID (JTI) for token revocation and tracking. */
    public String generateJti() {
        return UUID.randomUUID().toString();
    }

    /** Generates JWT token for user: creates access token with JTI, ISS, and AUD claims. */
    public String generateToken(Long userId, String email) {
        String jti = generateJti();
        return generateToken(userId, email, jti, null);
    }
    
    /** Generates JWT token with pre-generated JTI */
    public String generateToken(Long userId, String email, String jti) {
        return generateToken(userId, email, jti, null);
    }

    /**
     * Generate JWT token with JTI and device ID
     * 
     * <p>This method allows specifying a pre-generated JTI and device ID for device tracking.
     *
     * @param userId   User ID
     * @param email    User email
     * @param jti      Pre-generated JWT ID
     * @param deviceId Device ID for device tracking
     * @return JWT token string
     */
    public String generateToken(Long userId, String email, String jti, String deviceId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("jti", jti);
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            claims.put("deviceId", deviceId);
        }
        return createToken(claims, userId.toString());
    }

    /**
     * Generate refresh token for a user
     *
     * <p>Creates a refresh token with JTI, ISS, and AUD claims.
     *
     * @param userId User ID
     * @return Refresh token string
     */
    public String generateRefreshToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");
        // Generate unique JWT ID for token tracking
        String jti = generateJti();
        claims.put("jti", jti);
        // Refresh token expires in 7 days
        return createToken(claims, userId.toString(), expiration * 7);
    }
    
    /**
     * Generate refresh token with JTI (for use when JTI is already generated)
     * 
     * @param userId User ID
     * @param jti    Pre-generated JWT ID
     * @return Refresh token string
     */
    public String generateRefreshToken(Long userId, String jti) {
        return generateRefreshToken(userId, jti, null);
    }

    /**
     * Generate refresh token with JTI and device ID
     * 
     * <p>This method allows specifying a pre-generated JTI and device ID for device tracking.
     *
     * @param userId User ID
     * @param jti    Pre-generated JWT ID
     * @param deviceId Device ID for device tracking
     * @return Refresh token string
     */
    public String generateRefreshToken(Long userId, String jti, String deviceId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");
        claims.put("jti", jti);
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            claims.put("deviceId", deviceId);
        }
        // Refresh token expires in 7 days
        return createToken(claims, userId.toString(), expiration * 7);
    }

    /**
     * Generate signup token for phone number verification
     * 
     * <p>Creates a signup token (JWT) with configurable expiration (default: 15 minutes) for use after SMS verification.
     * The token contains the phone number and JTI for tracking.
     * Expiration time is configured via {@code sms.signup.token.expiration-minutes} property.
     *
     * @param phoneNumber Phone number that was verified
     * @param jti Pre-generated JWT ID
     * @return Signup token string (JWT with configurable expiration)
     */
    public String generateSignupToken(String phoneNumber, String jti) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("phoneNumber", phoneNumber);
        claims.put("type", "signup");
        claims.put("jti", jti);
        // Signup token expiration from configuration (converted from minutes to milliseconds)
        long signupTokenExpiration = signupTokenExpirationMinutes * 60L * 1000L;
        return createToken(claims, phoneNumber, signupTokenExpiration);
    }

    /**
     * Generate password reset token (JWT with phone number claim)
     * 
     * <p>Creates a temporary token for password reset.
     * The token expires in 30 minutes and contains the phone number.
     *
     * @param phoneNumber Phone number to include in token
     * @param jti JWT ID for token tracking
     * @return Reset token string (JWT with 30-minute expiration)
     */
    public String generateResetToken(String phoneNumber, String jti) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("phoneNumber", phoneNumber);
        claims.put("type", "password-reset");
        claims.put("jti", jti);
        // Reset token expires in 30 minutes
        long resetTokenExpiration = 30 * 60 * 1000L; // 30 minutes in milliseconds
        return createToken(claims, phoneNumber, resetTokenExpiration);
    }

    /**
     * Create JWT token with claims and subject
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return createToken(claims, subject, expiration);
    }

    /**
     * Create JWT token with claims, subject, and custom expiration
     * 
     * <p>Includes ISS (issuer) and AUD (audience) claims for security validation.
     */
    private String createToken(Map<String, Object> claims, String subject, Long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(issuer)  // ISS claim - identifies token issuer
                .audience().add(audience).and()  // AUD claim - identifies token audience
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Extract user ID from token
     */
    public Long getUserIdFromToken(String token) {
        String userIdStr = getClaimFromToken(token, Claims::getSubject);
        return userIdStr != null ? Long.parseLong(userIdStr) : null;
    }

    /**
     * Extract email from token
     */
    public String getEmailFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("email", String.class));
    }

    /**
     * Extract device ID from token
     * 
     * @param token JWT token
     * @return Device ID or null if not present
     */
    public String getDeviceIdFromToken(String token) {
        try {
            return getClaimFromToken(token, claims -> claims.get("deviceId", String.class));
        } catch (Exception e) {
            log.warn("Error extracting device ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract phone number from token
     * 
     * @param token JWT token
     * @return Phone number or null if not present
     */
    public String getPhoneNumberFromToken(String token) {
        try {
            return getClaimFromToken(token, claims -> claims.get("phoneNumber", String.class));
        } catch (Exception e) {
            log.warn("Error extracting phone number from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract expiration date from token
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * Extract a specific claim from token
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Get all claims from token
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Check if token is expired
     */
    public Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            log.warn("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Validate token
     *
     * @param token Token to validate
     * @param userId User ID to validate against
     * @return true if token is valid
     */
    public Boolean validateToken(String token, Long userId) {
        try {
            final Long tokenUserId = getUserIdFromToken(token);
            return (tokenUserId != null && tokenUserId.equals(userId) && !isTokenExpired(token));
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate token without user ID check
     * Validates both signature and expiration
     *
     * @param token Token to validate
     * @return true if token is valid (signature valid and not expired)
     */
    public Boolean validateToken(String token) {
        try {
            // Parse and verify token signature - this will throw if signature is invalid
            Claims claims = getAllClaimsFromToken(token);
            // Check expiration
            Date expiration = claims.getExpiration();
            return expiration != null && expiration.after(new Date());
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Extract JWT ID (JTI) from token
     * 
     * <p>JTI is used to uniquely identify a token for revocation and tracking.
     *
     * @param token JWT token
     * @return JWT ID (JTI) or null if not present
     */
    public String getJtiFromToken(String token) {
        try {
            return getClaimFromToken(token, claims -> claims.get("jti", String.class));
        } catch (Exception e) {
            log.warn("Error extracting JTI from token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Validate token issuer (ISS claim)
     * 
     * <p>Ensures the token was issued by the expected issuer (apart-token-backend).
     * This prevents accepting tokens from other systems.
     *
     * @param token JWT token to validate
     * @return true if issuer matches expected value, false otherwise
     */
    public Boolean validateIssuer(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            String tokenIssuer = claims.getIssuer();
            
            if (tokenIssuer == null) {
                log.debug("Token missing ISS claim");
                return false;
            }
            
            boolean isValid = issuer.equals(tokenIssuer);
            if (!isValid) {
                log.warn("Token issuer mismatch: expected={}, actual={}", issuer, tokenIssuer);
            }
            return isValid;
        } catch (Exception e) {
            log.warn("Error validating token issuer: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Validate token audience (AUD claim)
     * 
     * <p>Ensures the token is intended for the expected audience (apart-token-mobile-app).
     * This prevents accepting tokens intended for other applications.
     *
     * @param token JWT token to validate
     * @return true if audience matches expected value, false otherwise
     */
    public Boolean validateAudience(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            
            // Try getAudience() first (returns Set<String> or null)
            java.util.Set<String> audiences = claims.getAudience();
            if (audiences != null && !audiences.isEmpty()) {
                boolean isValid = audiences.contains(audience);
                if (!isValid) {
                    log.warn("Token audience mismatch: expected={}, actual={}", audience, audiences);
                }
                return isValid;
            }
            
            // Fallback: try get("aud") for backward compatibility
            Object audienceClaim = claims.get("aud");
            if (audienceClaim == null) {
                log.debug("Token missing AUD claim");
                return false;
            }
            
            // AUD can be a string or a list
            boolean isValid = false;
            if (audienceClaim instanceof String) {
                isValid = audience.equals(audienceClaim);
            } else if (audienceClaim instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<String> audList = (java.util.List<String>) audienceClaim;
                isValid = audList.contains(audience);
            }
            
            if (!isValid) {
                log.warn("Token audience mismatch: expected={}, actual={}", audience, audienceClaim);
            }
            return isValid;
        } catch (Exception e) {
            log.warn("Error validating token audience: {}", e.getMessage());
            return false;
        }
    }

}

