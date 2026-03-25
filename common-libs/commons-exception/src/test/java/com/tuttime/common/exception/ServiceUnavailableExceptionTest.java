package com.tuttime.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ServiceUnavailableException Tests")
class ServiceUnavailableExceptionTest {

	@Test
	@DisplayName("Should create exception with service name and message")
	void testConstructor_WithServiceNameAndMessage() {
		// Given
		String serviceName = "user-service";
		String message = "Service temporarily unavailable";

		// When
		ServiceUnavailableException exception = new ServiceUnavailableException(serviceName, message);

		// Then
		assertThat(exception).isNotNull();
		assertThat(exception.getServiceName()).isEqualTo(serviceName);
		assertThat(exception.getMessage()).isEqualTo(message);
		assertThat(exception.getCause()).isNull();
	}

	@Test
	@DisplayName("Should create exception with service name, message and cause")
	void testConstructor_WithServiceNameMessageAndCause() {
		// Given
		String serviceName = "payment-service";
		String message = "Service temporarily unavailable";
		Throwable cause = new RuntimeException("Connection timeout");

		// When
		ServiceUnavailableException exception = new ServiceUnavailableException(serviceName, message, cause);

		// Then
		assertThat(exception).isNotNull();
		assertThat(exception.getServiceName()).isEqualTo(serviceName);
		assertThat(exception.getMessage()).isEqualTo(message);
		assertThat(exception.getCause()).isEqualTo(cause);
	}

	@Test
	@DisplayName("Should return correct service name")
	void testGetServiceName_ReturnsCorrectValue() {
		// Given
		String serviceName = "notification-service";

		// When
		ServiceUnavailableException exception = new ServiceUnavailableException(serviceName, "Service unavailable");

		// Then
		assertThat(exception.getServiceName()).isEqualTo(serviceName);
	}

	@Test
	@DisplayName("Should handle null service name")
	void testConstructor_WithNullServiceName() {
		// Given
		String serviceName = null;
		String message = "Service unavailable";

		// When
		ServiceUnavailableException exception = new ServiceUnavailableException(serviceName, message);

		// Then
		assertThat(exception.getServiceName()).isNull();
		assertThat(exception.getMessage()).isEqualTo(message);
	}

	@Test
	@DisplayName("Should handle empty service name")
	void testConstructor_WithEmptyServiceName() {
		// Given
		String serviceName = "";
		String message = "Service unavailable";

		// When
		ServiceUnavailableException exception = new ServiceUnavailableException(serviceName, message);

		// Then
		assertThat(exception.getServiceName()).isEmpty();
		assertThat(exception.getMessage()).isEqualTo(message);
	}

	@Test
	@DisplayName("Should be instance of RuntimeException")
	void testInstanceOf_RuntimeException() {
		// When
		ServiceUnavailableException exception = new ServiceUnavailableException("test-service", "Test");

		// Then
		assertThat(exception).isInstanceOf(RuntimeException.class);
	}
}

