package com.tuttime.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthorizationException Tests")
class AuthorizationExceptionTest {

	@Test
	@DisplayName("Should create exception with message")
	void testConstructor_WithMessage() {
		// Given
		String message = "Access denied";

		// When
		AuthorizationException exception = new AuthorizationException(message);

		// Then
		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo(message);
		assertThat(exception.getCause()).isNull();
	}

	@Test
	@DisplayName("Should create exception with message and cause")
	void testConstructor_WithMessageAndCause() {
		// Given
		String message = "Access denied";
		Throwable cause = new RuntimeException("Root cause");

		// When
		AuthorizationException exception = new AuthorizationException(message, cause);

		// Then
		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo(message);
		assertThat(exception.getCause()).isEqualTo(cause);
	}

	@Test
	@DisplayName("Should be instance of RuntimeException")
	void testInstanceOf_RuntimeException() {
		// When
		AuthorizationException exception = new AuthorizationException("Test");

		// Then
		assertThat(exception).isInstanceOf(RuntimeException.class);
	}
}

