package com.tuttime.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException with correlation ID")
    void testHandleResourceNotFoundException_WithCorrelationId() {
        // Given
        String correlationId = "test-correlation-id";
        String path = "/api/v1/users/123";
        ResourceNotFoundException exception = new ResourceNotFoundException("User not found with ID: 123");

        when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);
        when(request.getRequestURI()).thenReturn(path);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceNotFoundException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(404);
        assertThat(errorResponse.getError()).isEqualTo("Resource Not Found");
        assertThat(errorResponse.getErrorCode()).isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND);
        assertThat(errorResponse.getMessage()).isEqualTo("User not found with ID: 123");
        assertThat(errorResponse.getPath()).isEqualTo(path);
        assertThat(errorResponse.getCorrelationId()).isEqualTo(correlationId);
        assertThat(errorResponse.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException without correlation ID (uses MDC)")
    void testHandleResourceNotFoundException_WithoutCorrelationId_UsesMDC() {
        // Given
        String correlationId = "mdc-correlation-id";
        String path = "/api/v1/users/123";
        ResourceNotFoundException exception = new ResourceNotFoundException("User not found");

        MDC.put("correlationId", correlationId);
        when(request.getHeader("X-Correlation-Id")).thenReturn(null);
        when(request.getRequestURI()).thenReturn(path);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceNotFoundException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getCorrelationId()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Should handle BusinessException correctly")
    void testHandleBusinessException() {
        // Given
        String correlationId = "test-correlation-id";
        String path = "/api/v1/test";
        BusinessException exception = new BusinessException("Business rule violation");

        when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);
        when(request.getRequestURI()).thenReturn(path);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBusinessException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(400);
        assertThat(errorResponse.getError()).isEqualTo("Business Rule Violation");
        assertThat(errorResponse.getErrorCode()).isEqualTo(ErrorCodes.BUSINESS_RULE_VIOLATION);
        assertThat(errorResponse.getMessageKey()).isNotNull(); // Auto-generated from message
        assertThat(errorResponse.getMessage()).isEqualTo("Business rule violation");
        assertThat(errorResponse.getCorrelationId()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Should handle BusinessException with messageKey correctly")
    void testHandleBusinessException_WithMessageKey() {
        // Given
        String correlationId = "test-correlation-id";
        String path = "/api/v1/test";
        String messageKey = "PHONE_NUMBER_REQUIRED";
        BusinessException exception = new BusinessException("Phone number is required", messageKey);

        when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);
        when(request.getRequestURI()).thenReturn(path);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBusinessException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(400);
        assertThat(errorResponse.getError()).isEqualTo("Business Rule Violation");
        assertThat(errorResponse.getErrorCode()).isEqualTo(ErrorCodes.BUSINESS_RULE_VIOLATION);
        assertThat(errorResponse.getMessageKey()).isEqualTo(messageKey);
        assertThat(errorResponse.getMessage()).isEqualTo("Phone number is required");
        assertThat(errorResponse.getCorrelationId()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Should handle ValidationException correctly")
    void testHandleValidationException() {
        // Given
        String correlationId = "test-correlation-id";
        String path = "/api/v1/test";
        ValidationException exception = new ValidationException("Validation failed");

        when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);
        when(request.getRequestURI()).thenReturn(path);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(400);
        assertThat(errorResponse.getError()).isEqualTo("Validation Failed");
        assertThat(errorResponse.getErrorCode()).isEqualTo(ErrorCodes.VALIDATION_FAILED);
        assertThat(errorResponse.getMessage()).isEqualTo("Validation failed");
        assertThat(errorResponse.getCorrelationId()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with field details")
    void testHandleValidationExceptions_WithFieldDetails() {
        // Given
        String correlationId = "test-correlation-id";
        String path = "/api/v1/users";
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        List<org.springframework.validation.ObjectError> errors = new ArrayList<>();

        FieldError fieldError1 = new FieldError("user", "email", "Email is required");
        FieldError fieldError2 = new FieldError("user", "password", "Password must be at least 8 characters");
        errors.add(fieldError1);
        errors.add(fieldError2);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(errors);
        when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);
        when(request.getRequestURI()).thenReturn(path);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationExceptions(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(400);
        assertThat(errorResponse.getError()).isEqualTo("Validation Failed");
        assertThat(errorResponse.getErrorCode()).isEqualTo(ErrorCodes.VALIDATION_FAILED);
        assertThat(errorResponse.getMessage()).isEqualTo("Request validation failed");
        assertThat(errorResponse.getCorrelationId()).isEqualTo(correlationId);
        assertThat(errorResponse.getDetails()).isNotNull();
        assertThat(errorResponse.getDetails()).containsKey("email");
        assertThat(errorResponse.getDetails()).containsKey("password");
        assertThat(errorResponse.getDetails().get("email")).isEqualTo("Email is required");
        assertThat(errorResponse.getDetails().get("password")).isEqualTo("Password must be at least 8 characters");
    }

    @Test
    @DisplayName("Should handle AuthenticationException correctly")
    void testHandleAuthenticationException() {
        // Given
        String correlationId = "test-correlation-id";
        String path = "/api/v1/auth/login";
        AuthenticationException exception = new AuthenticationException("Invalid credentials");

        when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);
        when(request.getRequestURI()).thenReturn(path);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAuthenticationException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(401);
        assertThat(errorResponse.getError()).isEqualTo("Unauthorized");
        assertThat(errorResponse.getErrorCode()).isEqualTo(ErrorCodes.UNAUTHORIZED);
        assertThat(errorResponse.getMessage()).isEqualTo("Invalid credentials");
        assertThat(errorResponse.getPath()).isEqualTo(path);
        assertThat(errorResponse.getCorrelationId()).isEqualTo(correlationId);
        assertThat(errorResponse.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should handle AuthorizationException correctly")
    void testHandleAuthorizationException() {
        // Given
        String correlationId = "test-correlation-id";
        String path = "/api/v1/admin/users";
        AuthorizationException exception = new AuthorizationException("Insufficient permissions");

        when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);
        when(request.getRequestURI()).thenReturn(path);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAuthorizationException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(403);
        assertThat(errorResponse.getError()).isEqualTo("Forbidden");
        assertThat(errorResponse.getErrorCode()).isEqualTo(ErrorCodes.FORBIDDEN);
        assertThat(errorResponse.getMessage()).isEqualTo("Insufficient permissions");
        assertThat(errorResponse.getPath()).isEqualTo(path);
        assertThat(errorResponse.getCorrelationId()).isEqualTo(correlationId);
        assertThat(errorResponse.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should handle RateLimitExceededException with Retry-After header")
    void testHandleRateLimitExceededException() {
        // Given
        String correlationId = "test-correlation-id";
        String path = "/api/v1/users";
        int retryAfterSeconds = 60;
        RateLimitExceededException exception = new RateLimitExceededException("Rate limit exceeded", retryAfterSeconds);

        when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);
        when(request.getRequestURI()).thenReturn(path);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRateLimitExceededException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo(String.valueOf(retryAfterSeconds));
        
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(429);
        assertThat(errorResponse.getError()).isEqualTo("Too Many Requests");
        assertThat(errorResponse.getErrorCode()).isEqualTo(ErrorCodes.TOO_MANY_REQUESTS);
        assertThat(errorResponse.getMessage()).isEqualTo("Rate limit exceeded");
        assertThat(errorResponse.getPath()).isEqualTo(path);
        assertThat(errorResponse.getCorrelationId()).isEqualTo(correlationId);
        assertThat(errorResponse.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should handle RateLimitExceededException with zero retry after seconds")
    void testHandleRateLimitExceededException_WithZeroRetryAfter() {
        // Given
        String correlationId = "test-correlation-id";
        String path = "/api/v1/users";
        int retryAfterSeconds = 0;
        RateLimitExceededException exception = new RateLimitExceededException("Rate limit exceeded", retryAfterSeconds);

        when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);
        when(request.getRequestURI()).thenReturn(path);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRateLimitExceededException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("0");
    }

    @Test
    @DisplayName("Should handle ServiceUnavailableException with service name in details")
    void testHandleServiceUnavailableException() {
        // Given
        String correlationId = "test-correlation-id";
        String path = "/api/v1/payments";
        String serviceName = "payment-service";
        ServiceUnavailableException exception = new ServiceUnavailableException(serviceName, "Service temporarily unavailable");

        when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);
        when(request.getRequestURI()).thenReturn(path);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleServiceUnavailableException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(503);
        assertThat(errorResponse.getError()).isEqualTo("Service Unavailable");
        assertThat(errorResponse.getErrorCode()).isEqualTo(ErrorCodes.SERVICE_UNAVAILABLE);
        assertThat(errorResponse.getMessage()).isEqualTo("Service temporarily unavailable");
        assertThat(errorResponse.getPath()).isEqualTo(path);
        assertThat(errorResponse.getCorrelationId()).isEqualTo(correlationId);
        assertThat(errorResponse.getDetails()).isNotNull();
        assertThat(errorResponse.getDetails()).containsKey("serviceName");
        assertThat(errorResponse.getDetails().get("serviceName")).isEqualTo(serviceName);
        assertThat(errorResponse.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should handle ServiceUnavailableException with null service name")
    void testHandleServiceUnavailableException_WithNullServiceName() {
        // Given
        String correlationId = "test-correlation-id";
        String path = "/api/v1/test";
        ServiceUnavailableException exception = new ServiceUnavailableException(null, "Service unavailable");

        when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);
        when(request.getRequestURI()).thenReturn(path);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleServiceUnavailableException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getDetails()).isNotNull();
        assertThat(errorResponse.getDetails()).containsKey("serviceName");
        assertThat(errorResponse.getDetails().get("serviceName")).isNull();
    }

    @Test
    @DisplayName("Should handle RuntimeException correctly")
    void testHandleRuntimeException() {
        // Given
        String correlationId = "test-correlation-id";
        String path = "/api/v1/test";
        RuntimeException exception = new RuntimeException("Unexpected runtime error");

        when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);
        when(request.getRequestURI()).thenReturn(path);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRuntimeException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(500);
        assertThat(errorResponse.getError()).isEqualTo("Internal Server Error");
        assertThat(errorResponse.getErrorCode()).isEqualTo(ErrorCodes.INTERNAL_SERVER_ERROR);
        assertThat(errorResponse.getMessage()).isEqualTo("Unexpected runtime error");
        assertThat(errorResponse.getCorrelationId()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Should handle RuntimeException with null message")
    void testHandleRuntimeException_WithNullMessage() {
        // Given
        String correlationId = "test-correlation-id";
        String path = "/api/v1/test";
        RuntimeException exception = new RuntimeException();

        when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);
        when(request.getRequestURI()).thenReturn(path);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRuntimeException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getMessage()).contains("RuntimeException");
    }

    @Test
    @DisplayName("Should handle generic Exception correctly")
    void testHandleGenericException() {
        // Given
        String correlationId = "test-correlation-id";
        String path = "/api/v1/test";
        Exception exception = new Exception("Generic error");

        when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);
        when(request.getRequestURI()).thenReturn(path);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGenericException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(500);
        assertThat(errorResponse.getError()).isEqualTo("Internal Server Error");
        assertThat(errorResponse.getErrorCode()).isEqualTo(ErrorCodes.INTERNAL_SERVER_ERROR);
        assertThat(errorResponse.getMessage()).isEqualTo("Generic error");
        assertThat(errorResponse.getCorrelationId()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Should include traceId from MDC when available")
    void testHandleException_WithTraceId() {
        // Given
        String correlationId = "test-correlation-id";
        String traceId = "test-trace-id";
        String path = "/api/v1/test";
        ResourceNotFoundException exception = new ResourceNotFoundException("Not found");

        MDC.put("traceId", traceId);
        when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);
        when(request.getRequestURI()).thenReturn(path);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceNotFoundException(exception, request);

        // Then
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getTraceId()).isEqualTo(traceId);
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException correctly")
    void testHandleIllegalArgumentException() {
        // Given
        String correlationId = "test-correlation-id";
        String path = "/api/v1/test";
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument provided");

        when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);
        when(request.getRequestURI()).thenReturn(path);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalArgumentException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(400);
        assertThat(errorResponse.getError()).isEqualTo("Bad Request");
        assertThat(errorResponse.getErrorCode()).isEqualTo(ErrorCodes.BUSINESS_RULE_VIOLATION);
        assertThat(errorResponse.getMessage()).isEqualTo("Invalid argument provided");
        assertThat(errorResponse.getPath()).isEqualTo(path);
        assertThat(errorResponse.getCorrelationId()).isEqualTo(correlationId);
        assertThat(response.getHeaders().getFirst("X-Correlation-Id")).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Should handle all error codes correctly - comprehensive test")
    void testAllErrorCodes() {
        String correlationId = "test-correlation-id";
        String path = "/api/v1/test";

        when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);
        when(request.getRequestURI()).thenReturn(path);

        // Test ResourceNotFoundException -> RESOURCE_NOT_FOUND
        ResponseEntity<ErrorResponse> response1 = globalExceptionHandler.handleResourceNotFoundException(
                new ResourceNotFoundException("Not found"), request);
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response1.getBody().getErrorCode()).isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND);

        // Test BusinessException -> BUSINESS_RULE_VIOLATION
        ResponseEntity<ErrorResponse> response2 = globalExceptionHandler.handleBusinessException(
                new BusinessException("Business error"), request);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response2.getBody().getErrorCode()).isEqualTo(ErrorCodes.BUSINESS_RULE_VIOLATION);

        // Test ValidationException -> VALIDATION_FAILED
        ResponseEntity<ErrorResponse> response3 = globalExceptionHandler.handleValidationException(
                new ValidationException("Validation error"), request);
        assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response3.getBody().getErrorCode()).isEqualTo(ErrorCodes.VALIDATION_FAILED);

        // Test AuthenticationException -> UNAUTHORIZED
        ResponseEntity<ErrorResponse> response4 = globalExceptionHandler.handleAuthenticationException(
                new AuthenticationException("Auth error"), request);
        assertThat(response4.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response4.getBody().getErrorCode()).isEqualTo(ErrorCodes.UNAUTHORIZED);

        // Test AuthorizationException -> FORBIDDEN
        ResponseEntity<ErrorResponse> response5 = globalExceptionHandler.handleAuthorizationException(
                new AuthorizationException("Authz error"), request);
        assertThat(response5.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response5.getBody().getErrorCode()).isEqualTo(ErrorCodes.FORBIDDEN);

        // Test RateLimitExceededException -> TOO_MANY_REQUESTS
        ResponseEntity<ErrorResponse> response6 = globalExceptionHandler.handleRateLimitExceededException(
                new RateLimitExceededException("Rate limit", 60), request);
        assertThat(response6.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response6.getBody().getErrorCode()).isEqualTo(ErrorCodes.TOO_MANY_REQUESTS);

        // Test ServiceUnavailableException -> SERVICE_UNAVAILABLE
        ResponseEntity<ErrorResponse> response7 = globalExceptionHandler.handleServiceUnavailableException(
                new ServiceUnavailableException("service", "Unavailable"), request);
        assertThat(response7.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response7.getBody().getErrorCode()).isEqualTo(ErrorCodes.SERVICE_UNAVAILABLE);

        // Test RuntimeException -> INTERNAL_SERVER_ERROR
        ResponseEntity<ErrorResponse> response8 = globalExceptionHandler.handleRuntimeException(
                new RuntimeException("Runtime error"), request);
        assertThat(response8.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response8.getBody().getErrorCode()).isEqualTo(ErrorCodes.INTERNAL_SERVER_ERROR);

        // Test Exception -> INTERNAL_SERVER_ERROR
        ResponseEntity<ErrorResponse> response9 = globalExceptionHandler.handleGenericException(
                new Exception("Generic error"), request);
        assertThat(response9.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response9.getBody().getErrorCode()).isEqualTo(ErrorCodes.INTERNAL_SERVER_ERROR);

        // Test IllegalArgumentException -> BUSINESS_RULE_VIOLATION
        ResponseEntity<ErrorResponse> response10 = globalExceptionHandler.handleIllegalArgumentException(
                new IllegalArgumentException("Illegal arg"), request);
        assertThat(response10.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response10.getBody().getErrorCode()).isEqualTo(ErrorCodes.BUSINESS_RULE_VIOLATION);
    }
}

