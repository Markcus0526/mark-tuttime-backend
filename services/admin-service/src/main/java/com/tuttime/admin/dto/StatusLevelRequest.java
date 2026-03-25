package com.tuttime.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Status Level Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusLevelRequest {

	@NotBlank(message = "Level name is required")
	@Size(max = 50, message = "Level name must not exceed 50 characters")
	private String levelName; // e.g., "SILVER", "GOLD", "PLATINUM", "ELITE", "XXX"

	@NotNull(message = "Minimum APART balance is required")
	@DecimalMin(value = "0", inclusive = true, message = "Minimum APART balance must be non-negative")
	private BigDecimal minApartBalance;

	@NotNull(message = "AP boost per hour is required")
	@DecimalMin(value = "0", inclusive = true, message = "AP boost per hour must be non-negative")
	private BigDecimal apBoostPerHour;

	private Boolean includesProAutoMining;

	private Integer displayOrder;

	private Boolean isActive;

	@Size(max = 1000, message = "Description must not exceed 1000 characters")
	private String description;
}

