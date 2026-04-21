package com.tuttime.booking.config;

import com.tuttime.common.auth.security.UserHeaderInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
	private final UserHeaderInterceptor userHeaderInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(userHeaderInterceptor)
				.addPathPatterns("/api/**") // Apply only to your API routes
				.excludePathPatterns("/actuator/**", "/swagger-ui/**"); // Skip system routes
	}
}