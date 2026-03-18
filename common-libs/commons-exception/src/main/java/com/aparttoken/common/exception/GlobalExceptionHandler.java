package com.tuttime.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/** Global Exception Handler: handles exceptions globally and returns consistent error responses with correlation ID and error codes. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        
        String correlationId = getCorrelationId(request);
        String path = request.getRequestURI();
        
        // Resource not found is a controlled exception - no stack trace needed
        log.error("Resource not found: correlationId={}, message={}, path={}", 
                correlationId, ex.getMessage(), path);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(HttpStatus.NOT_FOUND.value())
                .error("Resource Not Found")
                .errorCode(ErrorCodes.RESOURCE_NOT_FOUND)
                .message(ex.getMessage())
                .path(path)
                .correlationId(correlationId)
                .traceId(MDC.get("traceId"))
                .build();
        
        HttpHeaders headers = new HttpHeaders();
        if (correlationId != null) {
            headers.add(CORRELATION_ID_HEADER, correlationId);
        }
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .headers(headers)
                .body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        
        String correlationId = getCorrelationId(request);
        String path = request.getRequestURI();
        
        String messageKey = ex.getMessageKey();
        if (messageKey == null) {
            messageKey = generateMessageKey(ex.getMessage());
        }
        
        // For BusinessException, log without stack trace (it's a controlled exception)
        // Business exceptions are expected and don't need stack traces
        log.error("Business exception: correlationId={}, messageKey={}, message={}, path={}", 
                correlationId, messageKey, ex.getMessage(), path);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Business Rule Violation")
                .errorCode(ErrorCodes.BUSINESS_RULE_VIOLATION)
                .messageKey(messageKey)
                .message(ex.getMessage())
                .path(path)
                .correlationId(correlationId)
                .traceId(MDC.get("traceId"))
                .build();
        
        HttpHeaders headers = new HttpHeaders();
        if (correlationId != null) {
            headers.add(CORRELATION_ID_HEADER, correlationId);
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .headers(headers)
                .body(error);
    }
    
    /** Generates a message key from error message (fallback for backward compatibility). */
    private String generateMessageKey(String message) {
        if (message == null || message.trim().isEmpty()) {
            return CommonMessageKeys.UNKNOWN_ERROR;
        }
        
        // Convert message to key format: uppercase, replace spaces/special chars with underscores
        String key = message.toUpperCase()
                .replaceAll("[^A-Z0-9]", "_")
                .replaceAll("_{2,}", "_")
                .replaceAll("^_|_$", "");
        
        // If key is empty or too short, return unknown
        if (key.isEmpty() || key.length() < 3) {
            return CommonMessageKeys.UNKNOWN_ERROR;
        }
        
        return key;
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, HttpServletRequest request) {
        
        String correlationId = getCorrelationId(request);
        String path = request.getRequestURI();
        
        // Validation exceptions are controlled - no stack trace needed
        log.error("Validation exception: correlationId={}, message={}, path={}", 
                correlationId, ex.getMessage(), path);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .errorCode(ErrorCodes.VALIDATION_FAILED)
                .message(ex.getMessage())
                .path(path)
                .correlationId(correlationId)
                .traceId(MDC.get("traceId"))
                .build();
        
        HttpHeaders headers = new HttpHeaders();
        if (correlationId != null) {
            headers.add(CORRELATION_ID_HEADER, correlationId);
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .headers(headers)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        String correlationId = getCorrelationId(request);
        String path = request.getRequestURI();
        
        Map<String, Object> details = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            details.put(fieldName, errorMessage);
        });
        
        // Validation failures are controlled - no stack trace needed
        log.error("Validation failed: correlationId={}, fields={}, path={}", 
                correlationId, details.keySet(), path);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .errorCode(ErrorCodes.VALIDATION_FAILED)
                .message("Request validation failed")
                .path(path)
                .correlationId(correlationId)
                .traceId(MDC.get("traceId"))
                .details(details)
                .build();
        
        HttpHeaders headers = new HttpHeaders();
        if (correlationId != null) {
            headers.add(CORRELATION_ID_HEADER, correlationId);
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .headers(headers)
                .body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        
        String correlationId = getCorrelationId(request);
        String path = request.getRequestURI();
        
        // Authentication failures are controlled - no stack trace needed
        log.error("Authentication failed: correlationId={}, message={}, path={}", 
                correlationId, ex.getMessage(), path);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .errorCode(ErrorCodes.UNAUTHORIZED)
                .message(ex.getMessage())
                .path(path)
                .correlationId(correlationId)
                .traceId(MDC.get("traceId"))
                .build();
        
        HttpHeaders headers = new HttpHeaders();
        if (correlationId != null) {
            headers.add(CORRELATION_ID_HEADER, correlationId);
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .headers(headers)
                .body(error);
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationException(
            AuthorizationException ex, HttpServletRequest request) {
        
        String correlationId = getCorrelationId(request);
        String path = request.getRequestURI();
        
        // Authorization failures are controlled - no stack trace needed
        log.error("Authorization failed: correlationId={}, message={}, path={}", 
                correlationId, ex.getMessage(), path);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .errorCode(ErrorCodes.FORBIDDEN)
                .message(ex.getMessage())
                .path(path)
                .correlationId(correlationId)
                .traceId(MDC.get("traceId"))
                .build();
        
        HttpHeaders headers = new HttpHeaders();
        if (correlationId != null) {
            headers.add(CORRELATION_ID_HEADER, correlationId);
        }
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .headers(headers)
                .body(error);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceededException(
            RateLimitExceededException ex, HttpServletRequest request) {
        
        String correlationId = getCorrelationId(request);
        String path = request.getRequestURI();
        
        log.warn("Rate limit exceeded: correlationId={}, message={}, retryAfter={}, path={}", 
                correlationId, ex.getMessage(), ex.getRetryAfterSeconds(), path);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("Too Many Requests")
                .errorCode(ErrorCodes.TOO_MANY_REQUESTS)
                .message(ex.getMessage())
                .path(path)
                .correlationId(correlationId)
                .traceId(MDC.get("traceId"))
                .build();
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        if (correlationId != null) {
            headers.add(CORRELATION_ID_HEADER, correlationId);
        }
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .body(error);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailableException(
            ServiceUnavailableException ex, HttpServletRequest request) {
        
        String correlationId = getCorrelationId(request);
        String path = request.getRequestURI();
        
        // Service unavailable - include shortened stack trace (first 3 lines)
        String stackTraceSummary = StackTraceUtil.getStackTraceSummary(ex, 3);
        log.error("Service unavailable: correlationId={}, serviceName={}, message={}, path={}{}", 
                correlationId, ex.getServiceName(), ex.getMessage(), path,
                stackTraceSummary.isEmpty() ? "" : ", stackTrace=" + stackTraceSummary);
        
        Map<String, Object> details = new HashMap<>();
        details.put("serviceName", ex.getServiceName());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .errorCode(ErrorCodes.SERVICE_UNAVAILABLE)
                .message(ex.getMessage())
                .path(path)
                .correlationId(correlationId)
                .traceId(MDC.get("traceId"))
                .details(details)
                .build();
        
        HttpHeaders headers = new HttpHeaders();
        if (correlationId != null) {
            headers.add(CORRELATION_ID_HEADER, correlationId);
        }
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .headers(headers)
                .body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
        String correlationId = getCorrelationId(request);
        String path = request.getRequestURI();
        
        String message = ex.getMessage();
        String userFriendlyMessage = "Invalid request body format. Please ensure the request is valid JSON and Content-Type is application/json.";
        
        if (message != null) {
            if (message.contains("I/O error while reading input message")) {
                userFriendlyMessage = "Failed to read request body. Please check that the request is complete and properly formatted.";
            } else if (message.contains("JSON parse error") || message.contains("Unexpected character")) {
                userFriendlyMessage = "Invalid JSON format. Please check your request body syntax.";
            } else if (message.contains("Required request body is missing")) {
                userFriendlyMessage = "Request body is required but was not provided.";
            }
        }
        
        log.warn("Invalid request body: correlationId={}, message={}, path={}", 
                correlationId, message, path);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .errorCode(ErrorCodes.VALIDATION_FAILED)
                .message(userFriendlyMessage)
                .path(path)
                .correlationId(correlationId)
                .traceId(MDC.get("traceId"))
                .build();
        
        HttpHeaders headers = new HttpHeaders();
        if (correlationId != null) {
            headers.add(CORRELATION_ID_HEADER, correlationId);
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .headers(headers)
                .body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        String correlationId = getCorrelationId(request);
        String path = request.getRequestURI();
        
        log.warn("Illegal argument: correlationId={}, message={}, path={}", 
                correlationId, ex.getMessage(), path);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .errorCode(ErrorCodes.BUSINESS_RULE_VIOLATION)
                .message(ex.getMessage())
                .path(path)
                .correlationId(correlationId)
                .traceId(MDC.get("traceId"))
                .build();
        
        HttpHeaders headers = new HttpHeaders();
        if (correlationId != null) {
            headers.add(CORRELATION_ID_HEADER, correlationId);
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .headers(headers)
                .body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        
        String correlationId = getCorrelationId(request);
        String path = request.getRequestURI();
        
        String message = ex.getMessage();
        if (message == null || message.isEmpty()) {
            message = "An unexpected error occurred: " + ex.getClass().getSimpleName();
        }
        
        // For runtime exceptions, include shortened stack trace (first 5 lines)
        String stackTraceSummary = StackTraceUtil.getStackTraceSummary(ex, 5);
        log.error("Runtime exception: correlationId={}, message={}, path={}, stackTrace={}", 
                correlationId, message, path, stackTraceSummary);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .errorCode(ErrorCodes.INTERNAL_SERVER_ERROR)
                .message(message)
                .path(path)
                .correlationId(correlationId)
                .traceId(MDC.get("traceId"))
                .build();
        
        HttpHeaders headers = new HttpHeaders();
        if (correlationId != null) {
            headers.add(CORRELATION_ID_HEADER, correlationId);
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .headers(headers)
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        String correlationId = getCorrelationId(request);
        String path = request.getRequestURI();
        
        String message = ex.getMessage();
        if (message == null || message.isEmpty()) {
            message = "An unexpected error occurred: " + ex.getClass().getSimpleName();
        }
        
        // For unexpected errors, include shortened stack trace (first 5 lines)
        String stackTraceSummary = StackTraceUtil.getStackTraceSummary(ex, 5);
        log.error("Unexpected error: correlationId={}, message={}, path={}, stackTrace={}", 
                correlationId, message, path, stackTraceSummary);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .errorCode(ErrorCodes.INTERNAL_SERVER_ERROR)
                .message(message)
                .path(path)
                .correlationId(correlationId)
                .traceId(MDC.get("traceId"))
                .build();
        
        HttpHeaders headers = new HttpHeaders();
        if (correlationId != null) {
            headers.add(CORRELATION_ID_HEADER, correlationId);
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .headers(headers)
                .body(error);
    }

    /** Extracts correlation ID from request header or MDC context. */
    private String getCorrelationId(HttpServletRequest request) {
        if (request != null) {
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId != null && !correlationId.isEmpty()) {
                return correlationId;
            }
        }
        return MDC.get("correlationId");
    }
}

