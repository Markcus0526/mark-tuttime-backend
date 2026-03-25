package com.tuttime.user.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component // Added so Spring can inject the @Value properties
public class JwtUtil {

	@Value("${jwt.secret:secretkey12345678901234567890123456789012}") // HS512 needs a long key
	private String jwtSecret;

	@Value("${jwt.expiration:3600000}")
	private long jwtExpirationMs;

	// Convert the String secret into a proper SecretKey object
	private SecretKey getSigningKey() {
		byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	public String generateToken(UUID userId) {
		return Jwts.builder()
				.subject(userId.toString())
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
				.signWith(getSigningKey()) // Modern way to sign
				.compact();
	}

	public UUID getUserIdFromToken(String token) {
		String userId = Jwts.parser()
				.verifyWith(getSigningKey()) // Replaces setSigningKey
				.build()                    // FIX: This creates the parser
				.parseSignedClaims(token)    // Replaces parseClaimsJws
				.getPayload()                // Replaces getBody
				.getSubject();
		return UUID.fromString(userId);
	}

	public boolean validateJwtToken(String authToken) {
		try {
			Jwts.parser()
					.verifyWith(getSigningKey())
					.build()                    // FIX: This creates the parser
					.parseSignedClaims(authToken);
			return true;
		} catch (JwtException e) {
			// log error if needed
		}
		return false;
	}
}
