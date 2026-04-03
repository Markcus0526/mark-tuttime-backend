package com.tuttime.pos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request to sync Menu catalog from POS")
public record MenuSyncRequest(

		@NotNull @Schema(example = "88e7b8a0-2f3b-4c5d-ae6f-7890abcd1234")
		UUID restaurantId,

		@NotNull @Schema(example = "IIKO")
		String posSystem
) {
}
