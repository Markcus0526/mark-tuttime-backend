package com.tuttime.common.config;

import com.tuttime.common.annotation.ApiVersion;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

/**
 * API Version Handler Mapping
 * Enables version-based routing: /api/v1/..., /api/v2/...
 */
public class ApiVersionConfig extends RequestMappingHandlerMapping {

	@Override
	protected RequestCondition<?> getCustomTypeCondition(@NonNull Class<?> handlerType) {
		ApiVersion apiVersion = AnnotationUtils.findAnnotation(handlerType, ApiVersion.class);
		return createCondition(apiVersion);
	}

	@Override
	protected RequestCondition<?> getCustomMethodCondition(@NonNull Method method) {
		ApiVersion apiVersion = AnnotationUtils.findAnnotation(method, ApiVersion.class);
		return createCondition(apiVersion);
	}

	@Nullable
	private RequestCondition<?> createCondition(@Nullable ApiVersion apiVersion) {
		if (apiVersion == null) {
			return null;
		}
		return new ApiVersionRequestCondition(apiVersion.value());
	}

	private record ApiVersionRequestCondition(
			@NonNull String version) implements RequestCondition<ApiVersionRequestCondition> {

		@Override
		@NonNull
		public ApiVersionRequestCondition combine(@NonNull ApiVersionRequestCondition other) {
			return other;
		}

		@Override
		@Nullable
		public ApiVersionRequestCondition getMatchingCondition(@NonNull HttpServletRequest request) {
			String path = request.getRequestURI();
			if (path.contains("/api/" + version + "/")) {
				return this;
			}
			return null;
		}

		@Override
		public int compareTo(@NonNull ApiVersionRequestCondition other, @NonNull HttpServletRequest request) {
			return other.version.compareTo(this.version);
		}
	}
}

