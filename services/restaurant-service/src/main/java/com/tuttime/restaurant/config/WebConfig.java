package com.tuttime.restaurant.config;

import com.tuttime.common.auth.security.UserHeaderInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
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

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// This maps the URL path /media/ to your actual folder on the server
		registry.addResourceHandler("/media/**")
				.addResourceLocations("file:/1_DEVELOPMENT/Markcus0526/mark/tuttime-backend/");
	}
}