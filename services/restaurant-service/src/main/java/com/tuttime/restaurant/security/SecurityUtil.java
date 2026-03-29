package com.tuttime.restaurant.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("securityUtil")
public class SecurityUtil {

	public String getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof JwtAuthenticationToken) {
			JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
			// Adjust "userId" to the actual claim name in your JWT
			return jwtAuth.getToken().getClaimAsString("userId");
		}
		return null;
	}
}