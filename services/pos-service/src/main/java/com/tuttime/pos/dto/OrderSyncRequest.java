package com.tuttime.pos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Schema(description = "Request to trigger a manual Order sync")
public record OrderSyncRequest(

		@NotNull @Schema(example = "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6")
		UUID orderId,

		@NotNull @Schema(example = "RKEEPER")
		String posSystem
) {}