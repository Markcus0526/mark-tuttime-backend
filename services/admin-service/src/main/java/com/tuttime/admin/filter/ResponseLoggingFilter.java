package com.tuttime.admin.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * Filter to log HTTP responses for debugging.
 */
@Slf4j
@Component
@Order(1)
public class ResponseLoggingFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		if (request.getRequestURI().contains("/api/admin/system/config") &&
				!request.getRequestURI().contains("/test") &&
				!request.getRequestURI().contains("/minimal")) {

			ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

			try {
				filterChain.doFilter(request, responseWrapper);

				int status = responseWrapper.getStatus();
				byte[] responseBody = responseWrapper.getContentAsByteArray();
				String responseBodyString = new String(responseBody, responseWrapper.getCharacterEncoding());

				log.info("=== RESPONSE LOG: Status: {}, Body length: {} ===",
						status, responseBody.length);

				if (responseBodyString.length() > 500) {
					log.info("=== RESPONSE LOG: First 500 chars: {} ===", responseBodyString.substring(0, 500));
				} else {
					log.info("=== RESPONSE LOG: Full response: {} ===", responseBodyString);
				}

				responseWrapper.copyBodyToResponse();
			} catch (Exception e) {
				log.error("=== RESPONSE LOG ERROR: {} ===", e.getMessage(), e);
				throw e;
			}
		} else {
			filterChain.doFilter(request, response);
		}
	}
}

