package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Schema(description = "DTO for user location data")
public class UserLocationRequest {

	@Schema(description = "Latitude of the user's location", example = "37.7749")
	@NotNull(message = "Latitude is required")
	private Double latitude;

	@Schema(description = "Longitude of the user's location", example = "-122.4194")
	@NotNull(message = "Longitude is required")
	private Double longitude;

	@Schema(description = "Accuracy of the location in meters", example = "10.5", nullable = true)
	private Double accuracyMeters;

	@Schema(description = "Timestamp when location was recorded", example = "2024-01-01T12:00:00Z", nullable = true)
	private OffsetDateTime recordedAt;
}