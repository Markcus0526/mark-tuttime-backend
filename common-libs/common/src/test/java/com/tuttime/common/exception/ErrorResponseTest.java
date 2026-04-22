package com.tuttime.common.exception;

import com.tuttime.common.exception.ErrorCodes;
import com.tuttime.common.exception.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorResponse Tests")
class ErrorResponseTest {

	@Test
	@DisplayName("Should create ErrorResponse using builder pattern")
	void testBuilder_CreatesErrorResponse() {
		// Given
		OffsetDateTime timestamp = OffsetDateTime.now(ZoneOffset.UTC);
		String correlationId = "test-correlation-id";
		String traceId = "test-trace-id";
		Map<String, Object> details = new HashMap<>();
		details.put("field1", "error message 1");

		// When
		ErrorResponse errorResponse = ErrorResponse.builder()
				.timestamp(timestamp)
				.status(404)
				.error("Resource Not Found")
				.errorCode(ErrorCodes.RESOURCE_NOT_FOUND)
				.messageKey("USER_NOT_FOUND")
				.message("User not found")
				.path("/api/v1/users/123")
				.correlationId(correlationId)
				.traceId(traceId)
				.details(details)
				.build();

		// Then
		assertThat(errorResponse.getTimestamp()).isEqualTo(timestamp);
		assertThat(errorResponse.getStatus()).isEqualTo(404);
		assertThat(errorResponse.getError()).isEqualTo("Resource Not Found");
		assertThat(errorResponse.getErrorCode()).isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND);
		assertThat(errorResponse.getMessageKey()).isEqualTo("USER_NOT_FOUND");
		assertThat(errorResponse.getMessage()).isEqualTo("User not found");
		assertThat(errorResponse.getPath()).isEqualTo("/api/v1/users/123");
		assertThat(errorResponse.getCorrelationId()).isEqualTo(correlationId);
		assertThat(errorResponse.getTraceId()).isEqualTo(traceId);
		assertThat(errorResponse.getDetails()).isEqualTo(details);
	}

	@Test
	@DisplayName("Should create ErrorResponse with no-args constructor")
	void testNoArgsConstructor_CreatesErrorResponse() {
		// When
		ErrorResponse errorResponse = new ErrorResponse();

		// Then
		assertThat(errorResponse).isNotNull();
		assertThat(errorResponse.getTimestamp()).isNull();
		assertThat(errorResponse.getStatus()).isEqualTo(0);
		assertThat(errorResponse.getCorrelationId()).isNull();
	}

	@Test
	@DisplayName("Should create ErrorResponse with all-args constructor")
	void testAllArgsConstructor_CreatesErrorResponse() {
		// Given
		OffsetDateTime timestamp = OffsetDateTime.now(ZoneOffset.UTC);
		Map<String, Object> details = new HashMap<>();

		// When
		ErrorResponse errorResponse = new ErrorResponse(
				timestamp,
				400,
				"Bad Request",
				ErrorCodes.BAD_REQUEST,
				"INVALID_INPUT",  // messageKey
				"Invalid input",
				"/api/v1/test",
				"correlation-123",
				"trace-456",
				details
		);

		// Then
		assertThat(errorResponse.getTimestamp()).isEqualTo(timestamp);
		assertThat(errorResponse.getStatus()).isEqualTo(400);
		assertThat(errorResponse.getError()).isEqualTo("Bad Request");
		assertThat(errorResponse.getErrorCode()).isEqualTo(ErrorCodes.BAD_REQUEST);
		assertThat(errorResponse.getMessageKey()).isEqualTo("INVALID_INPUT");
		assertThat(errorResponse.getMessage()).isEqualTo("Invalid input");
		assertThat(errorResponse.getPath()).isEqualTo("/api/v1/test");
		assertThat(errorResponse.getCorrelationId()).isEqualTo("correlation-123");
		assertThat(errorResponse.getTraceId()).isEqualTo("trace-456");
		assertThat(errorResponse.getDetails()).isEqualTo(details);
	}

	@Test
	@DisplayName("Should handle null values in ErrorResponse")
	void testBuilder_WithNullValues_HandlesGracefully() {
		// When
		ErrorResponse errorResponse = ErrorResponse.builder()
				.status(500)
				.error("Internal Server Error")
				.build();

		// Then
		assertThat(errorResponse.getTimestamp()).isNull();
		assertThat(errorResponse.getStatus()).isEqualTo(500);
		assertThat(errorResponse.getError()).isEqualTo("Internal Server Error");
		assertThat(errorResponse.getErrorCode()).isNull();
		assertThat(errorResponse.getMessageKey()).isNull();
		assertThat(errorResponse.getMessage()).isNull();
		assertThat(errorResponse.getPath()).isNull();
		assertThat(errorResponse.getCorrelationId()).isNull();
		assertThat(errorResponse.getTraceId()).isNull();
		assertThat(errorResponse.getDetails()).isNull();
	}

	@Test
	@DisplayName("Should allow setting and getting all fields")
	void testSettersAndGetters_WorkCorrectly() {
		// Given
		ErrorResponse errorResponse = new ErrorResponse();
		OffsetDateTime timestamp = OffsetDateTime.now(ZoneOffset.UTC);
		Map<String, Object> details = new HashMap<>();
		details.put("test", "value");

		// When
		errorResponse.setTimestamp(timestamp);
		errorResponse.setStatus(403);
		errorResponse.setError("Forbidden");
		errorResponse.setErrorCode(ErrorCodes.FORBIDDEN);
		errorResponse.setMessageKey("ACCESS_DENIED");
		errorResponse.setMessage("Access denied");
		errorResponse.setPath("/api/v1/protected");
		errorResponse.setCorrelationId("corr-123");
		errorResponse.setTraceId("trace-456");
		errorResponse.setDetails(details);

		// Then
		assertThat(errorResponse.getTimestamp()).isEqualTo(timestamp);
		assertThat(errorResponse.getStatus()).isEqualTo(403);
		assertThat(errorResponse.getError()).isEqualTo("Forbidden");
		assertThat(errorResponse.getErrorCode()).isEqualTo(ErrorCodes.FORBIDDEN);
		assertThat(errorResponse.getMessageKey()).isEqualTo("ACCESS_DENIED");
		assertThat(errorResponse.getMessage()).isEqualTo("Access denied");
		assertThat(errorResponse.getPath()).isEqualTo("/api/v1/protected");
		assertThat(errorResponse.getCorrelationId()).isEqualTo("corr-123");
		assertThat(errorResponse.getTraceId()).isEqualTo("trace-456");
		assertThat(errorResponse.getDetails()).isEqualTo(details);
	}
}

