package com.tuttime.user.provider;

import com.tuttime.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private final SecretKey accessTokenKey;
    private final SecretKey refreshTokenKey;

    private final long accessTokenValiditySeconds;
    private final long refreshTokenValiditySeconds;

    @Value("${spring.security.jwt.issuer}")
    private String issuer;

    @Value("${spring.security.jwt.audience}")
    private String audience;

    public JwtTokenProvider(
            @Value("${spring.security.jwt.access-token.secret}") String accessSecret,
            @Value("${spring.security.jwt.refresh-token.secret}") String refreshSecret,
            @Value("${spring.security.jwt.access-token.expiration}") long accessExpiration,
            @Value("${spring.security.jwt.refresh-token.expiration}") long refreshExpiration) {

        this.accessTokenKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshTokenKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValiditySeconds = accessExpiration;
        this.refreshTokenValiditySeconds = refreshExpiration;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        String jti = generateJti();
        Map<String, Object> claims = generateTokenClaims(user, jti);

        return Jwts.builder()
                .id(jti)
                .claims(claims)
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenValiditySeconds)))
                .signWith(accessTokenKey)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        String jti = generateJti();
        Map<String, Object> claims = generateTokenClaims(user, jti);

        return Jwts.builder()
                .id(jti)
                .claims(claims)
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTokenValiditySeconds)))
                .signWith(refreshTokenKey)      // ✅ FIXED: use refreshTokenKey
                .compact();
    }

    /**
     * Validates the Access Token specifically using the accessTokenKey.
     */
    public boolean validateAccessToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(accessTokenKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validates the Refresh Token using the refreshTokenKey.
     */
    public boolean validateRefreshToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(refreshTokenKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extracts claims from the Access Token.
     */
    public Claims getAccessClaims(String token) {
        return Jwts.parser()
                .verifyWith(accessTokenKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserIdFromAccessToken(String token) {
        Claims claims = parseClaims(token, accessTokenKey);
        return UUID.fromString(claims.getSubject());
    }

    public UUID getUserIdFromRefreshToken(String token) {
        Claims claims = parseClaims(token, refreshTokenKey);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Generates unique JWT ID (JTI).
     */
    public String generateJti() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates JWT claims – no longer includes redundant 'iss' and 'aud'
     * because they are set by the builder.
     */
    public Map<String, Object> generateTokenClaims(User user, String jti) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.getId().toString());
        claims.put("userId", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("username", user.getFullName());
        // Store roles as a comma-separated string to match gateway's expectation
        String rolesStr = user.getRoles().stream()
                .map(role -> role.getRole().getName())
                .collect(Collectors.joining(","));
        claims.put("roles", rolesStr);
        claims.put("jti", jti);
        // ❌ Removed redundant "iss" and "aud" – they are now set in the builder
        return claims;
    }

    private Claims parseClaims(String token, SecretKey key) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token, SecretKey key) {
        final Date expiration = parseClaims(token, key).getExpiration();
        return expiration.before(new Date());
    }

    public long getAccessTokenExpirySeconds() {
        return accessTokenValiditySeconds;
    }

    public void invalidateRefreshToken(String refreshToken) {
        // Implement token blacklist or persistence deletion if needed
        // For stateless JWT, may store revoked tokens in cache or DB
    }

    public String getJtiFromToken(String token) {
        return getAccessClaims(token).getId();
    }

    public String getJtiFromRefreshToken(String token) {
        return getRefreshClaims(token).getId();
    }

    public Claims getRefreshClaims(String token) {
        return Jwts.parser()
                .verifyWith(refreshTokenKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getRefreshTokenExpirySeconds() {
        return refreshTokenValiditySeconds;
    }
}