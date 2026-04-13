package com.tuttime.restaurant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRequest {

	@NotNull(message = "Entity ID is required")
	private UUID entityId;

	@Min(value = 0, message = "dayOfWeek must be between 0 (Sunday) and 6 (Saturday)")
	@Max(value = 6, message = "dayOfWeek must be between 0 (Sunday) and 6 (Saturday)")
	private short dayOfWeek;

	@NotNull(message = "Open time is required")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
	@Schema(type = "string", example = "00:00:00", description = "Time in HH:mm:ss format")
	private LocalTime openTime;

	@NotNull(message = "Close time is required")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
	@Schema(type = "string", example = "00:00:00", description = "Time in HH:mm:ss format")
	private LocalTime closeTime;

	@NotNull
	private boolean closed;
}