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

import static com.tuttime.common.exception.ErrorResponseTestUtil.assertErrorResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Comprehensive integration tests for GlobalExceptionHandler.
 * 
 * <p>These tests verify that all error codes are correctly mapped to HTTP status codes
 * and that error responses follow the standardized format with correlation IDs.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Integration Tests - All Error Codes")
class GlobalExceptionHandlerIntegrationTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    private static final String CORRELATION_ID = "test-correlation-id-123";
    private static final String PATH = "/api/v1/test";

    @BeforeEach
    void setUp() {
        MDC.clear();
        when(request.getHeader("X-Correlation-Id")).thenReturn(CORRELATION_ID);
        when(request.getRequestURI()).thenReturn(PATH);
    }

    @Test
    @DisplayName("All error codes should be correctly mapped to HTTP status codes")
    void testAllErrorCodeMappings() {
        // Test ResourceNotFoundException -> RESOURCE_NOT_FOUND (404)
        ResponseEntity<ErrorResponse> response1 = globalExceptionHandler.handleResourceNotFoundException(
                new ResourceNotFoundException("Resource not found"), request);
        assertErrorResponse(response1, HttpStatus.NOT_FOUND, ErrorCodes.RESOURCE_NOT_FOUND, PATH, CORRELATION_ID);

        // Test BusinessException -> BUSINESS_RULE_VIOLATION (400)
        ResponseEntity<ErrorResponse> response2 = globalExceptionHandler.handleBusinessException(
                new BusinessException("Business rule violation"), request);
        assertErrorResponse(response2, HttpStatus.BAD_REQUEST, ErrorCodes.BUSINESS_RULE_VIOLATION, PATH, CORRELATION_ID);

        // Test ValidationException -> VALIDATION_FAILED (400)
        ResponseEntity<ErrorResponse> response3 = globalExceptionHandler.handleValidationException(
                new ValidationException("Validation failed"), request);
        assertErrorResponse(response3, HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, PATH, CORRELATION_ID);

        // Test AuthenticationException -> UNAUTHORIZED (401)
        ResponseEntity<ErrorResponse> response4 = globalExceptionHandler.handleAuthenticationException(
                new AuthenticationException("Unauthorized"), request);
        assertErrorResponse(response4, HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, PATH, CORRELATION_ID);

        // Test AuthorizationException -> FORBIDDEN (403)
        ResponseEntity<ErrorResponse> response5 = globalExceptionHandler.handleAuthorizationException(
                new AuthorizationException("Forbidden"), request);
        assertErrorResponse(response5, HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, PATH, CORRELATION_ID);

        // Test RateLimitExceededException -> TOO_MANY_REQUESTS (429)
        ResponseEntity<ErrorResponse> response6 = globalExceptionHandler.handleRateLimitExceededException(
                new RateLimitExceededException("Rate limit exceeded", 60), request);
        assertErrorResponse(response6, HttpStatus.TOO_MANY_REQUESTS, ErrorCodes.TOO_MANY_REQUESTS, PATH, CORRELATION_ID);
        assertThat(response6.getHeaders().getFirst("Retry-After")).isEqualTo("60");

        // Test ServiceUnavailableException -> SERVICE_UNAVAILABLE (503)
        ResponseEntity<ErrorResponse> response7 = globalExceptionHandler.handleServiceUnavailableException(
                new ServiceUnavailableException("service-name", "Service unavailable"), request);
        assertErrorResponse(response7, HttpStatus.SERVICE_UNAVAILABLE, ErrorCodes.SERVICE_UNAVAILABLE, PATH, CORRELATION_ID);

        // Test IllegalArgumentException -> BUSINESS_RULE_VIOLATION (400)
        ResponseEntity<ErrorResponse> response8 = globalExceptionHandler.handleIllegalArgumentException(
                new IllegalArgumentException("Invalid argument"), request);
        assertErrorResponse(response8, HttpStatus.BAD_REQUEST, ErrorCodes.BUSINESS_RULE_VIOLATION, PATH, CORRELATION_ID);

        // Test RuntimeException -> INTERNAL_SERVER_ERROR (500)
        ResponseEntity<ErrorResponse> response9 = globalExceptionHandler.handleRuntimeException(
                new RuntimeException("Runtime error"), request);
        assertErrorResponse(response9, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_SERVER_ERROR, PATH, CORRELATION_ID);

        // Test Exception -> INTERNAL_SERVER_ERROR (500)
        ResponseEntity<ErrorResponse> response10 = globalExceptionHandler.handleGenericException(
                new Exception("Generic error"), request);
        assertErrorResponse(response10, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_SERVER_ERROR, PATH, CORRELATION_ID);
    }

    @Test
    @DisplayName("All error responses should include X-Correlation-Id header")
    @SuppressWarnings("unchecked")
    void testAllErrorResponsesIncludeCorrelationIdHeader() {
        // Test all exception handlers include correlation ID header
        ResponseEntity<ErrorResponse>[] responses = (ResponseEntity<ErrorResponse>[]) new ResponseEntity[]{
                globalExceptionHandler.handleResourceNotFoundException(
                        new ResourceNotFoundException("Not found"), request),
                globalExceptionHandler.handleBusinessException(
                        new BusinessException("Business error"), request),
                globalExceptionHandler.handleValidationException(
                        new ValidationException("Validation error"), request),
                globalExceptionHandler.handleAuthenticationException(
                        new AuthenticationException("Auth error"), request),
                globalExceptionHandler.handleAuthorizationException(
                        new AuthorizationException("Authz error"), request),
                globalExceptionHandler.handleRateLimitExceededException(
                        new RateLimitExceededException("Rate limit", 60), request),
                globalExceptionHandler.handleServiceUnavailableException(
                        new ServiceUnavailableException("service", "Unavailable"), request),
                globalExceptionHandler.handleIllegalArgumentException(
                        new IllegalArgumentException("Illegal arg"), request),
                globalExceptionHandler.handleRuntimeException(
                        new RuntimeException("Runtime error"), request),
                globalExceptionHandler.handleGenericException(
                        new Exception("Generic error"), request)
        };

        for (ResponseEntity<ErrorResponse> response : responses) {
            assertThat(response.getHeaders().getFirst("X-Correlation-Id")).isEqualTo(CORRELATION_ID);
            assertThat(response.getBody().getCorrelationId()).isEqualTo(CORRELATION_ID);
        }
    }

    @Test
    @DisplayName("MethodArgumentNotValidException should include field details in error response")
    void testMethodArgumentNotValidException_IncludesFieldDetails() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        List<org.springframework.validation.ObjectError> errors = new ArrayList<>();

        FieldError fieldError1 = new FieldError("user", "email", "Email is required");
        FieldError fieldError2 = new FieldError("user", "password", "Password must be at least 8 characters");
        errors.add(fieldError1);
        errors.add(fieldError2);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(errors);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationExceptions(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        if (errorResponse != null) {
            assertThat(errorResponse.getErrorCode()).isEqualTo(ErrorCodes.VALIDATION_FAILED);
            assertThat(errorResponse.getDetails()).isNotNull();
            assertThat(errorResponse.getDetails()).containsKey("email");
            assertThat(errorResponse.getDetails()).containsKey("password");
            assertThat(errorResponse.getDetails().get("email")).isEqualTo("Email is required");
            assertThat(errorResponse.getDetails().get("password")).isEqualTo("Password must be at least 8 characters");
        }
    }

    @Test
    @DisplayName("ServiceUnavailableException should include service name in details")
    void testServiceUnavailableException_IncludesServiceName() {
        // Given
        String serviceName = "payment-service";
        ServiceUnavailableException exception = new ServiceUnavailableException(serviceName, "Service unavailable");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleServiceUnavailableException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        if (errorResponse != null) {
            assertThat(errorResponse.getDetails()).isNotNull();
            assertThat(errorResponse.getDetails()).containsKey("serviceName");
            assertThat(errorResponse.getDetails().get("serviceName")).isEqualTo(serviceName);
        }
    }

    @Test
    @DisplayName("RateLimitExceededException should include Retry-After header")
    void testRateLimitExceededException_IncludesRetryAfterHeader() {
        // Given
        int retryAfterSeconds = 120;
        RateLimitExceededException exception = new RateLimitExceededException("Rate limit exceeded", retryAfterSeconds);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRateLimitExceededException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo(String.valueOf(retryAfterSeconds));
    }

    @Test
    @DisplayName("All error responses should have consistent structure")
    void testAllErrorResponsesHaveConsistentStructure() {
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceNotFoundException(
                new ResourceNotFoundException("Test error"), request);

        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        
        // Verify all required fields are present
        if (errorResponse != null) {
            assertThat(errorResponse.getTimestamp()).isNotNull();
            assertThat(errorResponse.getStatus()).isGreaterThan(0);
            assertThat(errorResponse.getError()).isNotBlank();
            assertThat(errorResponse.getErrorCode()).isNotBlank();
            assertThat(errorResponse.getMessage()).isNotBlank();
            assertThat(errorResponse.getPath()).isNotBlank();
            assertThat(errorResponse.getCorrelationId()).isNotBlank();
        }
    }
}

