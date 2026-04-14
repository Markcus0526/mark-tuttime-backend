package com.tuttime.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.UUID;

public class ModifierDto {

	@Schema(description = "Modifier request details")
	public record ModifierRequest(

			@NotBlank
			String name,

			String description,

			BigDecimal priceAdjustment
	) {}

	@Schema(description = "Modifier response details")
	public record ModifierResponse(
			UUID modifierId,
			String name,
			String description,
			BigDecimal priceAdjustment
	) {}
}