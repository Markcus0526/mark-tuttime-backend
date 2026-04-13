package com.tuttime.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

	/**
	 * 1. Helper: The "Single Source of Truth" for creating responses.
	 */
	private ResponseEntity<ErrorResponse> createErrorResponse(
			HttpStatus status, String errorCode, String message,
			HttpServletRequest request, Map<String, Object> details) {

		String correlationId = getCorrelationId(request);

		ErrorResponse error = ErrorResponse.builder()
				.timestamp(OffsetDateTime.now(ZoneOffset.UTC))
				.status(status.value())
				.error(status.getReasonPhrase())
				.errorCode(errorCode)
				.message(message)
				.path(request.getRequestURI())
				.correlationId(correlationId)
				.traceId(MDC.get("traceId")) // Works with Micrometer/Sleuth
				.details(details)
				.build();

		return ResponseEntity.status(status)
				.header(CORRELATION_ID_HEADER, correlationId)
				.body(error);
	}

	/**
	 * 2. Business Logic Conflict (Your ConflictException fix)
	 */
	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
		log.warn("Business Conflict [{}]: {}", getCorrelationId(request), ex.getMessage());
		return createErrorResponse(
				HttpStatus.CONFLICT,
				ErrorCodes.BUSINESS_RULE_VIOLATION,
				ex.getMessage(),
				request,
				null);
	}

	/**
	 * 3. Validation Errors (DTO @Valid)
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		log.error("Validation failed [{}]: {}", getCorrelationId(request), ex.getMessage());
		return createErrorResponse(
				HttpStatus.BAD_REQUEST,
				ErrorCodes.VALIDATION_FAILED,
				ex.getMessage(),
				request,
				null);
	}

	/**
	 * 4. Resource Not Found
	 */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		log.error("Not Found [{}]: {}", getCorrelationId(request), ex.getMessage());
		return createErrorResponse(
				HttpStatus.NOT_FOUND,
				ErrorCodes.RESOURCE_NOT_FOUND,
				ex.getMessage(),
				request,
				null);
	}

	/**
	 * 5. Security / Unauthorized (Catching generic Security exceptions)
	 */
	@ExceptionHandler({AuthenticationException.class})
	public ResponseEntity<ErrorResponse> handleSecurity(Exception ex, HttpServletRequest request) {
		log.error("Security violation [{}]: {}", getCorrelationId(request), ex.getMessage());
		return createErrorResponse(
				HttpStatus.UNAUTHORIZED,
				ErrorCodes.UNAUTHORIZED,
				"Access denied",
				request,
				null);
	}

	/**
	 * 6. The Catch-All (Avoids leaking raw stack traces to users)
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGlobal(Exception ex, HttpServletRequest request) {
		log.error("UNEXPECTED ERROR [{}]: ", getCorrelationId(request), ex); // Log full stack trace for devs
		return createErrorResponse(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"INTERNAL_SERVER_ERROR",
				ex.getMessage(),
				request,
				null);
	}

	private String getCorrelationId(HttpServletRequest request) {
		String id = request.getHeader(CORRELATION_ID_HEADER);
		return (id != null) ? id : MDC.get("correlationId"); // Fallback to MDC
	}
}
