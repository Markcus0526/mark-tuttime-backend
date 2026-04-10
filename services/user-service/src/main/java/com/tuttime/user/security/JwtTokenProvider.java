package com.tuttime.user.security;

import com.tuttime.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenProvider {

	private final SecretKey accessTokenKey;
	private final SecretKey refreshTokenKey;

	private final long accessTokenValiditySeconds;
	private final long refreshTokenValiditySeconds;

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
		String id = generateJti();

		String[] rolesArray = user.getRoles().stream()
				.map(role -> role.getRole().getName())
				.toArray(String[]::new);
		Map<String, Object> claims = generateTokenClaims(user.getUserId(), user.getFullName(), rolesArray, id);

		return Jwts.builder()
				.id(id)
				.claims(claims)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(accessTokenValiditySeconds)))
				.signWith(accessTokenKey)
				.compact();
	}

	public String generateRefreshToken(User user) {
		Instant now = Instant.now();
		String id = generateJti();

		String[] rolesArray = user.getRoles().stream()
				.map(role -> role.getRole().getName())
				.toArray(String[]::new);
		Map<String, Object> claims = generateTokenClaims(user.getUserId(), user.getFullName(), rolesArray, id);

		return Jwts.builder()
				.id(id)
				.claims(claims)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(refreshTokenValiditySeconds)))
				.signWith(refreshTokenKey)
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
	 * Extracts claims from the Access Token.
	 */
	public Claims getAccessClaims(String token) {
		return Jwts.parser()
				.verifyWith(accessTokenKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	public boolean validateRefreshToken(String token) {
		try {
			return !isTokenExpired(token, refreshTokenKey);
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
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
	 * Generates JWT token with pre-generated JTI.
	 */
	public Map<String, Object> generateTokenClaims(UUID userId, String username, String[] roles, String jti) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("userId", userId);
		claims.put("username", username);
		claims.put("roles", roles);
		claims.put("jti", jti);

		return claims;
	}

	private Claims parseClaims(String token, SecretKey key) {
		return Jwts.parser()
				.verifyWith(key)             // Replaces setSigningKey
				.build()
				.parseSignedClaims(token)    // Replaces parseClaimsJws
				.getPayload();               // Replaces getBody
	}

	private boolean isTokenExpired(String token, SecretKey key) {
		final Date expiration = parseClaims(token, key).getExpiration();
		return expiration.before(new Date());
	}

	public long getAccessTokenExpirySeconds() {
		return accessTokenValiditySeconds;
	}

	public void invalidateRefreshToken(String refreshToken) {
		// Implement token blacklist or persistence deletion if applicable
		// For stateless JWT, might store revoked tokens in cache or DB
	}
}
