package com.tuttime.restaurant.dto;

import jakarta.validation.constraints.*;
import lombok.*;

// --- ScheduleRequest DTO ---
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleRequest {

	@NotNull(message = "Day of week must not be null")
	@Min(value = 0, message = "Day of week must be between 0 (Sunday) and 6 (Saturday)")
	@Max(value = 6, message = "Day of week must be between 0 (Sunday) and 6 (Saturday)")
	private Integer dayOfWeek;

	@NotNull(message = "Open time must not be null")
	private String openTime; // Could be LocalTime with custom deserializer

	@NotNull(message = "Close time must not be null")
	private String closeTime;

	@NotNull(message = "isClosed must not be null")
	private Boolean isClosed;

	// Getters and setters omitted for brevity
}