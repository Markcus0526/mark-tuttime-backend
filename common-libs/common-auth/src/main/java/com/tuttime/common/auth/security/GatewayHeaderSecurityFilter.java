package com.tuttime.common.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class GatewayHeaderSecurityFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request,
	                                HttpServletResponse response,
	                                FilterChain filterChain) throws ServletException, IOException {

		String userId = request.getHeader("X-User-Id");
		String rolesHeader = request.getHeader("X-User-Roles");

		// 1. Spring Security context (optional but fine)
		if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			List<SimpleGrantedAuthority> authorities = List.of();
			if (rolesHeader != null && !rolesHeader.isBlank()) {
				authorities = Arrays.stream(rolesHeader.split(","))
						.map(SimpleGrantedAuthority::new)
						.toList();
			}
			var authToken = new UsernamePasswordAuthenticationToken(userId, null, authorities);
			SecurityContextHolder.getContext().setAuthentication(authToken);
		}

		// 2. CRITICAL: Populate custom UserContext (used by your permission methods)
		if (userId != null && !userId.isBlank()) {
			UserContext.setUserId(userId);
		}
		if (rolesHeader != null && !rolesHeader.isBlank()) {
			UserContext.setRoles(rolesHeader);
		}

		try {
			filterChain.doFilter(request, response);
		} finally {
			// 3. Prevent memory leaks
			UserContext.clear();
		}
	}
}