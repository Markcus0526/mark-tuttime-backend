package com.tuttime.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Standard API response wrapper for all REST endpoints.
 *
 * <p><b>Timezone Note:</b> All timestamps in API responses are in UTC timezone.
 * The application is configured to use UTC across all layers (JVM, database, JSON serialization).
 *
 * @param <T> The type of data in the response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiRes<T> {
	private Boolean success;
	private String status;
	private String message;

	private T data;
	/**
	 * Response timestamp in UTC timezone.
	 * Generated using OffsetDateTime.now(ZoneOffset.UTC) to ensure UTC timezone consistency.
	 */
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime timestamp;

	public static <T> ApiRes<T> success(T data) {
		return ApiRes.<T>builder()
				.success(true)
				.data(data)
				.timestamp(OffsetDateTime.now(ZoneOffset.UTC))
				.build();
	}

	public static <T> ApiRes<T> success(String message, T data) {
		return ApiRes.<T>builder()
				.success(true)
				.message(message)
				.data(data)
				.timestamp(OffsetDateTime.now(ZoneOffset.UTC))
				.build();
	}

	public static <T> ApiRes<T> error(String message) {
		return ApiRes.<T>builder()
				.success(false)
				.message(message)
				.timestamp(OffsetDateTime.now(ZoneOffset.UTC))
				.build();
	}

	public static <T> ApiRes<T> error(String message, String errorCode) {
		return ApiRes.<T>builder()
				.success(false)
				.message(message)
				.status(errorCode)
				.timestamp(OffsetDateTime.now(ZoneOffset.UTC))
				.build();
	}
}
