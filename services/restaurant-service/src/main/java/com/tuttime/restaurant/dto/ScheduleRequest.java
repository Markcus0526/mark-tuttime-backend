package com.tuttime.restaurant.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRequest {

	@Min(value = 0, message = "dayOfWeek must be between 0 (Sunday) and 6 (Saturday)")
	@Max(value = 6, message = "dayOfWeek must be between 0 (Sunday) and 6 (Saturday)")
	private short dayOfWeek;

	@NotNull(message = "Open time is required")
	private LocalTime openTime;

	@NotNull(message = "Close time is required")
	private LocalTime closeTime;

	private boolean isClosed;
}