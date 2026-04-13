package com.tuttime.menu.dto;

import com.tuttime.menu.entity.enums.AppliesTo;
import com.tuttime.menu.entity.enums.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class DiscountDto {

	@Schema(description = "Discount request details")
	public record DiscountRequest(

			@NotBlank(message = "Discount name cannot be empty")
			String name,

			@NotNull DiscountType
			discountType,

			@NotNull @DecimalMin("0.00")
			BigDecimal amount,

			@NotNull AppliesTo
			appliesTo,

			UUID referenceId, // Category or Item ID

			OffsetDateTime startDate,

			OffsetDateTime endDate,

			Boolean active
	) {
	}

	@Schema(description = "Discount response details")
	public record DiscountResponse(
			UUID discountId,
			String name,
			DiscountType discountType,
			BigDecimal amount,
			AppliesTo appliesTo,
			UUID referenceId,
			OffsetDateTime startDate,
			OffsetDateTime endDate,
			Boolean active
	) {
	}
}