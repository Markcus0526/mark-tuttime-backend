package com.tuttime.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.auth.security.UserHeaderInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
	private final UserHeaderInterceptor userHeaderInterceptor;
	private final ObjectMapper objectMapper;

	@Value("${app.media.local.upload-dir}")
	private String uploadDir;

	@Value("${app.media.local.upload-url}")
	private String uploadUrl;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(userHeaderInterceptor)
				.addPathPatterns("/api/**") // Apply only to your API routes
				.excludePathPatterns("/actuator/**", "/swagger-ui/**"); // Skip system routes
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		Path path = Paths.get(uploadDir).toAbsolutePath();
		String location = "file:" + path + "/";

		registry.addResourceHandler(uploadUrl + "**")
				.addResourceLocations(location);
	}

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		// Create a converter that supports octet-stream for JSON mapping
		AbstractJackson2HttpMessageConverter octetStreamConverter =
				new AbstractJackson2HttpMessageConverter(objectMapper, org.springframework.http.MediaType.APPLICATION_OCTET_STREAM) {
				};

		converters.add(octetStreamConverter);
	}
}