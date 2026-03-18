package com.aparttoken.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthenticationException Tests")
class AuthenticationExceptionTest {

    @Test
    @DisplayName("Should create exception with message")
    void testConstructor_WithMessage() {
        // Given
        String message = "Authentication failed";

        // When
        AuthenticationException exception = new AuthenticationException(message);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    void testConstructor_WithMessageAndCause() {
        // Given
        String message = "Authentication failed";
        Throwable cause = new RuntimeException("Root cause");

        // When
        AuthenticationException exception = new AuthenticationException(message, cause);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Should be instance of RuntimeException")
    void testInstanceOf_RuntimeException() {
        // When
        AuthenticationException exception = new AuthenticationException("Test");

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}

