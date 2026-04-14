package com.tuttime.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public class ModifierGroupDto {

	@Schema(description = "Modifier Group request details")
	public record ModifierGroupRequest(
			@NotBlank
			String name,

			String description,

			@Min(0)
			Integer minSelect,

			@Min(1)
			Integer maxSelect
	) {}

	@Schema(description = "Modifier Group response details")
	public record ModifierGroupResponse(
			UUID modifierGroupId,
			String name,
			String description,
			Integer minSelect,
			Integer maxSelect,
			List<ModifierDto.ModifierResponse> modifiers
	) {}
}
