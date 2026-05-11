package com.tuttime.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.exception.ErrorCodes;
import com.tuttime.common.exception.StackTraceUtil;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@Order(-2)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

	private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
	private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	private static final String SWAGGER_UI_HTML = "/swagger-ui.html";
	private static final String SWAGGER_UI = "/swagger-ui";
	private static final String SWAGGER_UI_INDEX = "/swagger-ui/index.html";

	private final ObjectMapper objectMapper;

	public GlobalExceptionHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public Mono<Void> handle(ServerWebExchange exchange, @NonNull Throwable ex) {
		ServerHttpResponse response = exchange.getResponse();

		// Already committed → do nothing
		if (response.isCommitted()) {
			log.debug("Response already committed, ignoring exception: {}", ex.getMessage());
			return Mono.empty();
		}

		String path = exchange.getRequest().getURI().getPath();

		// Swagger redirect
		if (SWAGGER_UI_HTML.equals(path) || SWAGGER_UI.equals(path)) {
			if (!response.isCommitted()) {
				response.setStatusCode(HttpStatus.FOUND);
				response.getHeaders().setLocation(URI.create(SWAGGER_UI_INDEX));
				return response.setComplete();
			}
			return Mono.empty();
		}

		// Let Swagger assets pass through
		if (path.contains("swagger-ui") || path.contains("v3/api-docs") || path.contains("webjars")) {
			return Mono.error(ex);
		}

		// Determine HTTP status and message from exception type
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		String message = "An unexpected error occurred";

		if (ex instanceof ResponseStatusException rse) {
			status = HttpStatus.valueOf(rse.getStatusCode().value());
			message = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
		} else if (ex instanceof WebExchangeBindException) {
			status = HttpStatus.BAD_REQUEST;
			message = "Validation failed: " + ex.getMessage();
		} else if (ex instanceof ServerWebInputException) {
			status = HttpStatus.BAD_REQUEST;
			message = "Invalid request parameters";
		} else if (ex instanceof TimeoutException) {
			status = HttpStatus.GATEWAY_TIMEOUT;
			message = "Request timeout";
		} else if (ex instanceof java.net.SocketTimeoutException) {
			status = HttpStatus.GATEWAY_TIMEOUT;
			message = "Connection timeout";
		} else if (ex instanceof java.net.ConnectException) {
			status = HttpStatus.BAD_GATEWAY;
			message = "Unable to connect to upstream service";
		} else if (ex instanceof NotFoundException) {
			status = HttpStatus.NOT_FOUND;
			message = "Resource not found";
		}

		// Map status to error code (single source of truth)
		String errorCode = mapStatusToErrorCode(status);
		String correlationId = getCorrelationId(exchange);

		// Log error
		String stackTraceSummary = StackTraceUtil.getStackTraceSummary(ex, 5);
		log.error("Global exception handler: correlationId={}, errorCode={}, message={}, path={}, stackTrace={}",
				correlationId, errorCode, message, path, stackTraceSummary);

		// Build error response body
		Map<String, Object> errorResponse = new HashMap<>();
		errorResponse.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC).format(ISO_FORMATTER));
		errorResponse.put("status", status.value());
		errorResponse.put("error", status.getReasonPhrase());
		errorResponse.put("errorCode", errorCode);
		errorResponse.put("message", message);
		errorResponse.put("path", path);
		errorResponse.put("correlationId", correlationId);

		// Final committed check before writing
		if (response.isCommitted()) {
			log.debug("Response became committed before writing error, skipping");
			return Mono.empty();
		}

		response.setStatusCode(status);
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		if (correlationId != null) {
			response.getHeaders().add(CORRELATION_ID_HEADER, correlationId);
		}

		try {
			String json = objectMapper.writeValueAsString(errorResponse);
			DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
			return response.writeWith(Mono.just(buffer));
		} catch (Exception e) {
			log.error("Failed to serialize error response: {}", e.getMessage());
			return Mono.empty();
		}
	}

	private String getCorrelationId(ServerWebExchange exchange) {
		String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
		if (correlationId == null || correlationId.isEmpty()) {
			Object attr = exchange.getAttributes().get(CORRELATION_ID_HEADER);
			correlationId = (attr instanceof String) ? (String) attr : null;
		}
		return correlationId;
	}

	private String mapStatusToErrorCode(HttpStatus status) {
		return switch (status.value()) {
			case 400 -> ErrorCodes.BAD_REQUEST;
			case 401 -> ErrorCodes.UNAUTHORIZED;
			case 403 -> ErrorCodes.FORBIDDEN;
			case 404 -> ErrorCodes.RESOURCE_NOT_FOUND;
			case 405 -> ErrorCodes.METHOD_NOT_ALLOWED;
			case 406 -> ErrorCodes.NOT_ACCEPTABLE;
			case 409 -> ErrorCodes.CONFLICT;
			case 412 -> ErrorCodes.PRECONDITION_FAILED;
			case 413 -> ErrorCodes.PAYLOAD_TOO_LARGE;
			case 414 -> ErrorCodes.URI_TOO_LONG;
			case 415 -> ErrorCodes.UNSUPPORTED_MEDIA_TYPE;
			case 422 -> ErrorCodes.UNPROCESSABLE_ENTITY;
			case 423 -> ErrorCodes.LOCKED;
			case 429 -> ErrorCodes.TOO_MANY_REQUESTS;
			case 500 -> ErrorCodes.INTERNAL_SERVER_ERROR;
			case 501 -> ErrorCodes.NOT_IMPLEMENTED;
			case 502 -> ErrorCodes.BAD_GATEWAY;
			case 503 -> ErrorCodes.SERVICE_UNAVAILABLE;
			case 504 -> ErrorCodes.GATEWAY_TIMEOUT;
			default -> ErrorCodes.UNKNOWN_ERROR;
		};
	}
}