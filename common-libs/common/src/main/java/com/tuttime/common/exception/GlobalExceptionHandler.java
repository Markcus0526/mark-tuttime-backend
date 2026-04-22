package com.tuttime.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

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

	//-------- Standard Exception ------------//

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGlobal(Exception ex, HttpServletRequest request) {
		log.error(">>>>> UNEXPECTED ERROR [{}]: ", getCorrelationId(request), ex); // Log full stack trace for devs
		return createErrorResponse(
				HttpStatus.INTERNAL_SERVER_ERROR,
				ex.getMessage(),
				ex.getMessage(),
				request,
				null);
	}

	/**
	 * Validation Errors (DTO @Valid)
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining(", "));

		log.error(">>>>> MethodArgumentNotValidException: [{}]: {}", getCorrelationId(request), ex.getMessage());
		return createErrorResponse(
				HttpStatus.BAD_REQUEST,
				ErrorCodes.INVALID_INPUT,
				message,
				request,
				null);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleJsonParseError(HttpMessageNotReadableException ex, HttpServletRequest request) {
		log.error(">>>>> HttpMessageNotReadableException: [{}]: {}", getCorrelationId(request), ex.getMessage());
		return createErrorResponse(
				NOT_FOUND,
				ErrorCodes.RESOURCE_NOT_FOUND,
				ex.getMessage(),
				request,
				null);
	}

	//-------- Customized Exception ------------//

	/**
	 * Business Logic Conflict (Your ConflictException fix)
	 */
	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
		log.error(">>>>> ConflictException: [{}]: {}", getCorrelationId(request), ex.getMessage());
		return createErrorResponse(
				HttpStatus.CONFLICT,
				ErrorCodes.CONFLICT,
				ex.getMessage(),
				request,
				null);
	}

	/**
	 * Security / Unauthorized (Catching generic Security exceptions)
	 */
	@ExceptionHandler({ForbiddenException.class})
	public ResponseEntity<ErrorResponse> handleForbidden(Exception ex, HttpServletRequest request) {
		log.error(">>>>> ForbiddenException: [{}]: {}", getCorrelationId(request), ex.getMessage());
		return createErrorResponse(
				HttpStatus.FORBIDDEN,
				ErrorCodes.FORBIDDEN,
				ex.getMessage(),
				request,
				null);
	}

	@ExceptionHandler({UnauthorizedException.class})
	public ResponseEntity<ErrorResponse> handleUnauthorized(Exception ex, HttpServletRequest request) {
		log.error(">>>>> UnauthorizedException: [{}]: {}", getCorrelationId(request), ex.getMessage());
		return createErrorResponse(
				HttpStatus.UNAUTHORIZED,
				ErrorCodes.UNAUTHORIZED,
				ex.getMessage(),
				request,
				null);
	}

	/**
	 * Resource Not Found
	 */
	@ExceptionHandler(InvalidRequestException.class)
	public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException ex, HttpServletRequest request) {
		log.error(">>>>> InvalidRequestException: [{}]: {}", getCorrelationId(request), ex.getMessage());
		return createErrorResponse(
				HttpStatus.BAD_REQUEST,
				ErrorCodes.VALIDATION_FAILED,
				ex.getMessage(),
				request,
				null);
	}

	/**
	 * Handle gRPC specific exceptions (e.g., from Restaurant or Order service)
	 */
	@ExceptionHandler(StatusRuntimeException.class)
	public ResponseEntity<ErrorResponse> handleStatusRuntimeException(StatusRuntimeException ex, HttpServletRequest request) {
		Status grpcStatus = ex.getStatus();

		log.error(">>>>> gRPC External Error [{}]: {} - {}",
				getCorrelationId(request),
				grpcStatus.getCode(),
				grpcStatus.getDescription());

		// Map gRPC codes to your specific business ErrorCodes
		String customErrorCode = switch (grpcStatus.getCode()) {
			case UNAVAILABLE -> "SERVICE_OFFLINE";   // Specific to TutTime
			case DEADLINE_EXCEEDED -> "SERVICE_RESPONSE_TIMEOUT";
			case NOT_FOUND -> "SERVICE_NOT_FOUND";
			default -> "EXTERNAL_SERVICE_ERROR_" + grpcStatus.getCode().name();
		};

		// Map gRPC Status Codes to HTTP Status Codes
		HttpStatus httpStatus = switch (grpcStatus.getCode()) {
			case NOT_FOUND -> HttpStatus.NOT_FOUND;
			case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
			case DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT;
			case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
			default -> HttpStatus.INTERNAL_SERVER_ERROR;
		};

		// Matches your existing createErrorResponse signature
		return createErrorResponse(
				httpStatus,
				customErrorCode,
				grpcStatus.getDescription(),
				request,
				null);
	}

	/**
	 * POS-specific
	 */
	@ExceptionHandler(PosSyncException.class)
	public ResponseEntity<ErrorResponse> handlePosSyncException(PosSyncException ex, HttpServletRequest request) {
		log.error(">>>>> PosSyncException: [{}]: {}", getCorrelationId(request), ex.getMessage());
		return createErrorResponse(
				HttpStatus.BAD_REQUEST,
				ErrorCodes.VALIDATION_FAILED,
				ex.getMessage(),
				request,
				null);
	}

	/**
	 * Business Logic Conflict (Your BusinessException fix)
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
		log.error(">>>>> BusinessException: [{}]: {}", getCorrelationId(request), ex.getMessage());
		return createErrorResponse(
				HttpStatus.INTERNAL_SERVER_ERROR,
				ErrorCodes.INTERNAL_SERVER_ERROR,
				ex.getMessage(),
				request,
				null);
	}


	private String getCorrelationId(HttpServletRequest request) {
		String id = request.getHeader(CORRELATION_ID_HEADER);
		return (id != null) ? id : MDC.get("correlationId"); // Fallback to MDC
	}
}
