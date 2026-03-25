package com.tuttime.admin.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Admin JWT Token Utility: handles JWT token generation and validation for admin users. Includes ISS/AUD validation as mandatory security requirement.
 */
@Slf4j
@Component
public class AdminJwtTokenUtil {

	@Value("${spring.security.jwt.secret}")
	private String secret;

	@Value("${spring.security.jwt.expiration:3600000}")
	private Long expiration;

	@Value("${spring.security.jwt.issuer:tuttime-admin-backend}")
	private String issuer;

	@Value("${spring.security.jwt.audience:tuttime-admin-panel}")
	private String audience;

	/**
	 * Gets secret key from configured secret string.
	 */
	private SecretKey getSigningKey() {
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
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

	/**
	 * Generates unique JWT ID (JTI).
	 */
	public String generateJti() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Generates JWT token for admin user.
	 */
	public String generateToken(Long adminId, String username, String role) {
		String jti = generateJti();
		return generateToken(adminId, username, role, jti);
	}

	/**
	 * Generates JWT token with pre-generated JTI.
	 */
	public String generateToken(Long adminId, String username, String role, String jti) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("adminId", adminId);
		claims.put("username", username);
		claims.put("role", role);
		claims.put("jti", jti);

		return createToken(claims, jti);
	}

	/**
	 * Creates JWT token with claims.
	 */
	private String createToken(Map<String, Object> claims, String jti) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + expiration);

		return Jwts.builder()
				.claims(claims)
				.id(jti)
				.issuer(issuer) // MANDATORY: ISS claim
				.audience().add(audience).and() // MANDATORY: AUD claim
				.issuedAt(now)
				.expiration(expiryDate)
				.signWith(getSigningKey())
				.compact();
	}

	/**
	 * Extract JTI from token
	 */
	public String getJtiFromToken(String token) {
		return getClaimFromToken(token, Claims::getId);
	}

	/**
	 * Extract admin ID from token
	 */
	public Long getAdminIdFromToken(String token) {
		Claims claims = getAllClaimsFromToken(token);
		Object adminIdObj = claims.get("adminId");
		if (adminIdObj instanceof Number) {
			return ((Number) adminIdObj).longValue();
		}
		return null;
	}

	/**
	 * Extract username from token
	 */
	public String getUsernameFromToken(String token) {
		Claims claims = getAllClaimsFromToken(token);
		return claims.get("username", String.class);
	}

	/**
	 * Extract role from token
	 */
	public String getRoleFromToken(String token) {
		Claims claims = getAllClaimsFromToken(token);
		return claims.get("role", String.class);
	}

	/**
	 * Extract expiration date from token
	 */
	public Date getExpirationDateFromToken(String token) {
		return getClaimFromToken(token, Claims::getExpiration);
	}

	/**
	 * Extract claim from token
	 */
	private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
		Claims claims = getAllClaimsFromToken(token);
		return claimsResolver.apply(claims);
	}

	/**
	 * Get all claims from token
	 *
	 * <p>Explicitly parses as JWS (signed) token, not JWE (encrypted).
	 * JWS tokens have 3 parts (header.payload.signature), while JWE tokens have 5 parts.
	 */
	private Claims getAllClaimsFromToken(String token) {
		try {
			// Explicitly parse as JWS (signed) token
			// JWS format: header.payload.signature (3 parts separated by dots)
			// JWE format: header.encrypted_key.iv.ciphertext.tag (5 parts separated by dots)
			String[] parts = token.split("\\.");
			if (parts.length != 3) {
				log.error("Invalid JWT token format: expected 3 parts (JWS), got {} parts. Token length: {}",
						parts.length, token.length());
				throw new io.jsonwebtoken.MalformedJwtException(
						"Invalid JWT token format: expected JWS (3 parts), got " + parts.length + " parts. " +
								"This token appears to be JWE (encrypted) or malformed. " +
								"Expected format: header.payload.signature");
			}

			return Jwts.parser()
					.verifyWith(getSigningKey())
					.build()
					.parseSignedClaims(token)
					.getPayload();
		} catch (io.jsonwebtoken.MalformedJwtException e) {
			// Re-throw with more context
			if (e.getMessage() != null && e.getMessage().contains("JWE")) {
				log.error("JWT parsing error: Token appears to be JWE (encrypted) but we expect JWS (signed). " +
								"Token length: {}, First 50 chars: {}",
						token.length(),
						token.length() > 50 ? token.substring(0, 50) : token);
				throw new io.jsonwebtoken.MalformedJwtException(
						"Token format error: Expected JWS (signed) token, but received JWE (encrypted) token. " +
								"JWS tokens have 3 parts (header.payload.signature), while JWE tokens have 5 parts. " +
								"Please ensure the token is a signed JWT, not an encrypted JWT.", e);
			}
			throw e;
		} catch (Exception e) {
			log.error("Failed to parse JWT token: {} (token length: {})",
					e.getMessage(), token != null ? token.length() : 0);
			throw e;
		}
	}

	/**
	 * Check if token is expired
	 */
	private Boolean isTokenExpired(String token) {
		Date expiration = getExpirationDateFromToken(token);
		return expiration.before(new Date());
	}

	/**
	 * Validate token issuer (ISS) - MANDATORY
	 */
	public boolean validateIssuer(String token) {
		try {
			Claims claims = getAllClaimsFromToken(token);
			String tokenIssuer = claims.getIssuer();
			return issuer.equals(tokenIssuer);
		} catch (Exception e) {
			log.debug("Token issuer validation failed: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Validate token audience (AUD) - MANDATORY
	 */
	public boolean validateAudience(String token) {
		try {
			Claims claims = getAllClaimsFromToken(token);
			Object aud = claims.get("aud");

			if (aud == null) {
				log.warn("Token audience claim is null");
				return false;
			}

			// Handle String audience
			if (aud instanceof String) {
				boolean matches = audience.equals(aud);
				if (!matches) {
					log.warn("Token audience mismatch: token has '{}', expected '{}'", aud, audience);
				}
				return matches;
			}

			// Handle Collection types (List, Set, LinkedHashSet, etc.)
			if (aud instanceof java.util.Collection) {
				@SuppressWarnings("unchecked")
				java.util.Collection<String> audiences = (java.util.Collection<String>) aud;
				boolean contains = audiences.contains(audience);
				if (!contains) {
					log.warn("Token audience not in collection: token has {}, expected '{}'", audiences, audience);
				}
				return contains;
			}

			log.warn("Token audience is unsupported type: {} (value: {})",
					aud.getClass().getName(), aud);
			return false;
		} catch (Exception e) {
			log.warn("Token audience validation failed: {}", e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Validate token signature and expiration
	 */
	public boolean validateTokenSignature(String token) {
		try {
			getAllClaimsFromToken(token);
			return !isTokenExpired(token);
		} catch (Exception e) {
			log.debug("Token signature validation failed: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Full token validation (signature, expiration, ISS, AUD)
	 * This does NOT check blacklist/whitelist - that's done separately
	 */
	public boolean validateToken(String token) {
		return validateTokenSignature(token)
				&& validateIssuer(token)
				&& validateAudience(token);
	}

	/**
	 * Get token expiration time in milliseconds
	 */
	public Long getExpiration() {
		return expiration;
	}
}

