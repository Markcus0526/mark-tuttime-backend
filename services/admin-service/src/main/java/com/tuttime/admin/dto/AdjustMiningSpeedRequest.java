package com.tuttime.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Adjust Mining Speed Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustMiningSpeedRequest {
	@NotNull(message = "Speed is required")
	private Double speed; // AP/hour

	@NotBlank(message = "Reason is required")
	private String reason;
}

