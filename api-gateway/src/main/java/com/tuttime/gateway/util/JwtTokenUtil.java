package com.tuttime.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

/**
 * JWT Token Utility: provides JWT token validation and claim extraction. Must use same secret/validation logic as user-service.
 */
@Slf4j
@Component
public class JwtTokenUtil {

	@Value("${spring.security.jwt.secret:tuttime-secret-key-change-in-production-min-256-bits}")
	private String secret;

	@Value("${spring.security.jwt.issuer:tuttime-backend}")
	private String issuer;

	@Value("${spring.security.jwt.audience:tuttime-mobile-app}")
	private String audience;

	/**
	 * Get secret key from the configured secret string (matches user-service logic).
	 */
	private SecretKey getSigningKey() {
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < 32) {
			byte[] paddedKey = new byte[32];
			System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
			// Fill remaining bytes with secret repeated
			for (int i = keyBytes.length; i < 32; i++) {
				paddedKey[i] = keyBytes[i % keyBytes.length];
			}
			keyBytes = paddedKey;
		}
		return Keys.hmacShaKeyFor(keyBytes);
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
	 * Extract a specific claim from token
	 */
	public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = getAllClaimsFromToken(token);
		return claimsResolver.apply(claims);
	}

	/**
	 * Extract user ID from token
	 */
	public UUID getUserIdFromToken(String token) {
		try {
			String userIdStr = getClaimFromToken(token, Claims::getSubject);
			return userIdStr != null ? Long.parseLong(userIdStr) : null;
		} catch (Exception e) {
			log.warn("Error extracting user ID from token: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * Extract email from token
	 */
	public String getEmailFromToken(String token) {
		return getClaimFromToken(token, claims -> claims.get("email", String.class));
	}

	/**
	 * Extract email from token
	 */
	public String getRoleFromToken(String token) {
		return getClaimFromToken(token, claims -> claims.get("role", String.class));
	}

	/**
	 * Extract JWT ID (JTI) from token
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
	 * Extract expiration date from token
	 */
	public Date getExpirationDateFromToken(String token) {
		return getClaimFromToken(token, Claims::getExpiration);
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
	 * Validate token signature and expiration
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
	 * Validate token issuer (ISS claim)
	 *
	 * <p>Ensures the token was issued by the expected issuer (tuttime-backend).
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
	 * <p>Ensures the token is intended for the expected audience (tuttime-mobile-app).
	 * This prevents accepting tokens intended for other applications.
	 *
	 * @param token JWT token to validate
	 * @return true if audience matches expected value, false otherwise
	 */
	public Boolean validateAudience(String token) {
		try {
			Claims claims = getAllClaimsFromToken(token);

			// Use getAudience() which returns Set<String> (proper way to get AUD claim)
			java.util.Set<String> audiences = claims.getAudience();
			if (audiences == null || audiences.isEmpty()) {
				log.debug("Token missing AUD claim");
				return false;
			}

			boolean isValid = audiences.contains(audience);
			if (!isValid) {
				log.warn("Token audience mismatch: expected={}, actual={}", audience, audiences);
			}
			return isValid;
		} catch (Exception e) {
			log.warn("Error validating token audience: {}", e.getMessage());
			return false;
		}
	}
}

