package com.tuttime.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utility class for testing error responses.
 * Provides helper methods to validate error response structure and content.
 */
public class ErrorResponseTestUtil {

    /**
     * Validates that an error response has the correct structure and values.
     *
     * @param response The ResponseEntity containing the error response
     * @param expectedStatus The expected HTTP status code
     * @param expectedErrorCode The expected error code string
     * @param expectedPath The expected request path
     * @param correlationId The expected correlation ID (can be null)
     */
    public static void assertErrorResponse(
            ResponseEntity<ErrorResponse> response,
            HttpStatus expectedStatus,
            String expectedErrorCode,
            String expectedPath,
            String correlationId) {

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);

        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(expectedStatus.value());
        assertThat(errorResponse.getErrorCode()).isEqualTo(expectedErrorCode);
        assertThat(errorResponse.getPath()).isEqualTo(expectedPath);
        assertThat(errorResponse.getTimestamp()).isNotNull();

        if (correlationId != null) {
            assertThat(errorResponse.getCorrelationId()).isEqualTo(correlationId);
            assertThat(response.getHeaders().getFirst("X-Correlation-Id")).isEqualTo(correlationId);
        } else {
            // Correlation ID should still be present (from MDC or generated)
            assertThat(errorResponse.getCorrelationId()).isNotNull();
        }
    }

    /**
     * Validates that an error response has the correct structure without checking correlation ID.
     */
    public static void assertErrorResponse(
            ResponseEntity<ErrorResponse> response,
            HttpStatus expectedStatus,
            String expectedErrorCode,
            String expectedPath) {
        assertErrorResponse(response, expectedStatus, expectedErrorCode, expectedPath, null);
    }

    /**
     * Validates that an error response contains a specific message.
     */
    public static void assertErrorMessage(
            ResponseEntity<ErrorResponse> response,
            String expectedMessage) {
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(expectedMessage);
    }

    /**
     * Validates that an error response contains details.
     */
    public static void assertErrorDetails(
            ResponseEntity<ErrorResponse> response,
            String expectedKey,
            Object expectedValue) {
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetails()).isNotNull();
        assertThat(response.getBody().getDetails()).containsKey(expectedKey);
        assertThat(response.getBody().getDetails().get(expectedKey)).isEqualTo(expectedValue);
    }

    /**
     * Validates that a response header is present with the expected value.
     */
    public static void assertHeader(
            ResponseEntity<ErrorResponse> response,
            String headerName,
            String expectedValue) {
        assertThat(response.getHeaders().getFirst(headerName)).isEqualTo(expectedValue);
    }
}

