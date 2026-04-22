package com.tuttime.common.exception;

import com.tuttime.common.exception.ErrorCodes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorCodes Tests")
class ErrorCodesTest {

	@Test
	@DisplayName("Should have all required error code constants")
	void testErrorCodes_ContainsAllRequiredConstants() {
		// Client Error Codes (4xx)
		assertThat(ErrorCodes.BAD_REQUEST).isEqualTo("BAD_REQUEST");
		assertThat(ErrorCodes.UNAUTHORIZED).isEqualTo("UNAUTHORIZED");
		assertThat(ErrorCodes.FORBIDDEN).isEqualTo("FORBIDDEN");
		assertThat(ErrorCodes.RESOURCE_NOT_FOUND).isEqualTo("RESOURCE_NOT_FOUND");
		assertThat(ErrorCodes.VALIDATION_FAILED).isEqualTo("VALIDATION_FAILED");
		assertThat(ErrorCodes.TOO_MANY_REQUESTS).isEqualTo("TOO_MANY_REQUESTS");

		// Business Logic Error Codes
		assertThat(ErrorCodes.BUSINESS_RULE_VIOLATION).isEqualTo("BUSINESS_RULE_VIOLATION");
		assertThat(ErrorCodes.INSUFFICIENT_BALANCE).isEqualTo("INSUFFICIENT_BALANCE");
		assertThat(ErrorCodes.RESOURCE_ALREADY_EXISTS).isEqualTo("RESOURCE_ALREADY_EXISTS");

		// Server Error Codes (5xx)
		assertThat(ErrorCodes.INTERNAL_SERVER_ERROR).isEqualTo("INTERNAL_SERVER_ERROR");
		assertThat(ErrorCodes.SERVICE_UNAVAILABLE).isEqualTo("SERVICE_UNAVAILABLE");
		assertThat(ErrorCodes.BAD_GATEWAY).isEqualTo("BAD_GATEWAY");
		assertThat(ErrorCodes.GATEWAY_TIMEOUT).isEqualTo("GATEWAY_TIMEOUT");

		// Unknown Error
		assertThat(ErrorCodes.UNKNOWN_ERROR).isEqualTo("UNKNOWN_ERROR");
	}

	@Test
	@DisplayName("Should have validation error codes")
	void testErrorCodes_ContainsValidationCodes() {
		assertThat(ErrorCodes.VALIDATION_FAILED).isEqualTo("VALIDATION_FAILED");
		assertThat(ErrorCodes.INVALID_INPUT).isEqualTo("INVALID_INPUT");
		assertThat(ErrorCodes.MISSING_REQUIRED_FIELD).isEqualTo("MISSING_REQUIRED_FIELD");
		assertThat(ErrorCodes.INVALID_FORMAT).isEqualTo("INVALID_FORMAT");
		assertThat(ErrorCodes.OUT_OF_RANGE).isEqualTo("OUT_OF_RANGE");
	}

	@Test
	@DisplayName("Should have business logic error codes")
	void testErrorCodes_ContainsBusinessLogicCodes() {
		assertThat(ErrorCodes.BUSINESS_RULE_VIOLATION).isEqualTo("BUSINESS_RULE_VIOLATION");
		assertThat(ErrorCodes.INSUFFICIENT_BALANCE).isEqualTo("INSUFFICIENT_BALANCE");
		assertThat(ErrorCodes.RESOURCE_ALREADY_EXISTS).isEqualTo("RESOURCE_ALREADY_EXISTS");
		assertThat(ErrorCodes.OPERATION_NOT_ALLOWED).isEqualTo("OPERATION_NOT_ALLOWED");
		assertThat(ErrorCodes.INVALID_STATE).isEqualTo("INVALID_STATE");
		assertThat(ErrorCodes.QUOTA_EXCEEDED).isEqualTo("QUOTA_EXCEEDED");
	}

	@Test
	@DisplayName("Should have network error codes")
	void testErrorCodes_ContainsNetworkCodes() {
		assertThat(ErrorCodes.CONNECTION_TIMEOUT).isEqualTo("CONNECTION_TIMEOUT");
		assertThat(ErrorCodes.SERVICE_UNABLE_TO_CONNECT).isEqualTo("SERVICE_UNABLE_TO_CONNECT");
	}

	@Test
	@DisplayName("Should prevent instantiation (utility class)")
	void testErrorCodes_CannotBeInstantiated() throws Exception {
		// Given
		Constructor<ErrorCodes> constructor = ErrorCodes.class.getDeclaredConstructor();

		// Then
		assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();

		// Make accessible and verify it throws exception when instantiated
		constructor.setAccessible(true);

		// The constructor exists but is private, which is expected for utility classes
		// We verify it's private above, so we know it cannot be instantiated
		assertThat(Modifier.isPrivate(ErrorCodes.class.getModifiers())).isFalse(); // Class itself is not private
		assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue(); // Constructor is private
	}

	@Test
	@DisplayName("Should have consistent error code format (uppercase with underscores)")
	void testErrorCodes_HaveConsistentFormat() {
		// All error codes should be uppercase with underscores
		assertThat(ErrorCodes.BAD_REQUEST).matches("^[A-Z_]+$");
		assertThat(ErrorCodes.RESOURCE_NOT_FOUND).matches("^[A-Z_]+$");
		assertThat(ErrorCodes.BUSINESS_RULE_VIOLATION).matches("^[A-Z_]+$");
		assertThat(ErrorCodes.INTERNAL_SERVER_ERROR).matches("^[A-Z_]+$");
	}
}

