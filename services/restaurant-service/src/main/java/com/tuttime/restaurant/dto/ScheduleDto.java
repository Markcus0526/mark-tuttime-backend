package com.tuttime.restaurant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ScheduleDto {

	@Schema(description = "Schedule request details")
	public record ScheduleRequest(

			@NotNull(message = "Entity ID is required")
			UUID entityId,

			@Min(value = 0, message = "dayOfWeek must be between 0 (Sunday) and 6 (Saturday)")
			@Max(value = 6, message = "dayOfWeek must be between 0 (Sunday) and 6 (Saturday)")
			short dayOfWeek,

			@NotNull(message = "Open time is required")
			@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
			@Schema(type = "string", example = "00:00:00", description = "Time in HH:mm:ss format")
			LocalTime openTime,

			@NotNull(message = "Close time is required")
			@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
			@Schema(type = "string", example = "00:00:00", description = "Time in HH:mm:ss format")
			LocalTime closeTime,

			@NotNull
			boolean closed
	) {
	}

	@Schema(description = "Schedule request details")
	public record ScheduleResponse(

			UUID scheduleId,
			UUID entityId,
			int dayOfWeek, // 0=Sunday ... 6=Saturday
			LocalTime openTime,
			LocalTime closeTime,
			boolean closed,
			OffsetDateTime createdAt,
			OffsetDateTime updatedAt
	) {
	}
}
