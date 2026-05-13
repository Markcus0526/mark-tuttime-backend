package com.tuttime.menu.dto;

import com.tuttime.menu.entity.enums.AppliesTo;
import com.tuttime.menu.entity.enums.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class DiscountDto {

	@Schema(description = "Discount request details")
	public record DiscountRequest(

			@NotNull
			UUID branchId,

			@NotBlank(message = "Discount name cannot be empty")
			String name,

			@NotNull DiscountType
			discountType,

			@NotNull @DecimalMin("0.00")
			BigDecimal amount,

			@NotNull
			AppliesTo appliesTo,

			UUID referenceId, // Category or Item ID

			OffsetDateTime startDate,

			OffsetDateTime endDate,

			Boolean active
	) {
		@AssertTrue(message = "Percentage discount must be between 0 and 100")
		public boolean isPercentageAmountValid() {
			if (discountType == DiscountType.PERCENTAGE && amount != null) {
				return amount.compareTo(BigDecimal.ZERO) >= 0 && amount.compareTo(new BigDecimal("100")) <= 0;
			}
			return true;
		}

		@AssertTrue(message = "Fixed amount discount cannot be negative")
		public boolean isFixedAmountValid() {
			if (discountType == DiscountType.FIXED_AMOUNT && amount != null) {
				return amount.compareTo(BigDecimal.ZERO) >= 0;
			}
			return true;
		}

		@AssertTrue(message = "Start date must be before or equal to end date")
		public boolean isDateRangeValid() {
			if (startDate != null && endDate != null) {
				return !startDate.isAfter(endDate);
			}
			return true;
		}
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

	/**
	 * Result of price calculation including the name of the applied discount
	 * for the "Promotion Applied" section in Figma.
	 */
	public record PricingResult(
			BigDecimal finalPrice,
			String discountName
	) {
	}
}