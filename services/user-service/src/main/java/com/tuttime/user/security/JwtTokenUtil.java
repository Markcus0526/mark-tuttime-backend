package com.tuttime.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenUtil {

	private final SecretKey accessTokenKey;
	private final SecretKey refreshTokenKey;

	private final long accessTokenValiditySeconds;
	private final long refreshTokenValiditySeconds;

	public JwtTokenUtil(
			@Value("${spring.security.jwt.access-token.secret}") String accessSecret,
			@Value("${spring.security.jwt.refresh-token.secret}") String refreshSecret,
			@Value("${spring.security.jwt.access-token.expiration}") long accessExpiration,
			@Value("${spring.security.jwt.refresh-token.expiration}") long refreshExpiration) {

		this.accessTokenKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
		this.refreshTokenKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenValiditySeconds = accessExpiration;
		this.refreshTokenValiditySeconds = refreshExpiration;
	}

	public String generateAccessToken(UUID userId, String fullName) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(userId.toString())
				.claim("name", fullName)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(accessTokenValiditySeconds)))
				.signWith(accessTokenKey)
				.compact();
	}

	public String generateRefreshToken(UUID userId) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(userId.toString())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(refreshTokenValiditySeconds)))
				.signWith(accessTokenKey)
				.compact();
	}

	public boolean validateAccessToken(String token, UserDetails userDetails) {
		try {
			String userId = getUserIdFromAccessToken(token).toString();
			return userId.equals(userDetails.getUsername()) && !isTokenExpired(token, accessTokenKey);
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
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
