package com.tuttime.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimitExceededException Tests")
class RateLimitExceededExceptionTest {

    @Test
    @DisplayName("Should create exception with message and retry after seconds")
    void testConstructor_WithMessageAndRetryAfter() {
        // Given
        String message = "Rate limit exceeded";
        int retryAfterSeconds = 60;

        // When
        RateLimitExceededException exception = new RateLimitExceededException(message, retryAfterSeconds);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getRetryAfterSeconds()).isEqualTo(retryAfterSeconds);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("Should create exception with message, retry after seconds and cause")
    void testConstructor_WithMessageRetryAfterAndCause() {
        // Given
        String message = "Rate limit exceeded";
        int retryAfterSeconds = 30;
        Throwable cause = new RuntimeException("Root cause");

        // When
        RateLimitExceededException exception = new RateLimitExceededException(message, retryAfterSeconds, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getRetryAfterSeconds()).isEqualTo(retryAfterSeconds);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Should return correct retry after seconds")
    void testGetRetryAfterSeconds_ReturnsCorrectValue() {
        // Given
        int retryAfterSeconds = 120;

        // When
        RateLimitExceededException exception = new RateLimitExceededException("Rate limit exceeded", retryAfterSeconds);

        // Then
        assertThat(exception.getRetryAfterSeconds()).isEqualTo(retryAfterSeconds);
    }

    @Test
    @DisplayName("Should handle zero retry after seconds")
    void testConstructor_WithZeroRetryAfter() {
        // Given
        int retryAfterSeconds = 0;

        // When
        RateLimitExceededException exception = new RateLimitExceededException("Rate limit exceeded", retryAfterSeconds);

        // Then
        assertThat(exception.getRetryAfterSeconds()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should be instance of RuntimeException")
    void testInstanceOf_RuntimeException() {
        // When
        RateLimitExceededException exception = new RateLimitExceededException("Test", 60);

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}

