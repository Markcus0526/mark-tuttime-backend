package com.tuttime.common.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserHeaderInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
		String userId = request.getHeader("X-User-Id");
		String roles = request.getHeader("X-User-Roles");

		// Always populate the context so that getter methods are safe to call on public routes
		UserContext.setUserId(userId);
		UserContext.setRoles(roles); // The updated UserContext automatically handles null/empty strings

		return true;
	}

	@Override
	public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
		UserContext.clear(); // Critical to avoid memory leaks!
	}
}
