package com.tuttime.pos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Configuration for a restaurant's POS system")
public record PosCredentialsDto(

		@NotNull
		UUID restaurantId,

		@NotNull @Schema(example = "IIKO")
		String posSystem,

		@NotNull @Schema(example = "https://iiko.services")
		String apiUrl,

		String apiKey,

		String apiToken
) {}