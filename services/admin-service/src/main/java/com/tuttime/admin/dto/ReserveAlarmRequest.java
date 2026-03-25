package com.tuttime.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Reserve Alarm Request DTO
 *
 * <p>Matches API specification:
 * - threshold (BigDecimal) - Threshold value
 * - isActive (Boolean) - Whether alarm is active
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveAlarmRequest {

	private String network; // null means all networks

	@NotBlank(message = "Alarm type is required")
	private String alarmType; // LOW_BALANCE, PRICE_DROP, PRICE_INCREASE, HIGH_VOLATILITY, SUPPLY_THRESHOLD, NAV_THRESHOLD

	@NotNull(message = "Threshold value is required")
	private BigDecimal threshold; // API field name

	@Builder.Default
	private Boolean isActive = true; // API field name

	// Optional fields for advanced configuration
	@Builder.Default
	private String comparisonOperator = "LT"; // LT, GT, EQ, LTE, GTE (defaults to LT for backward compatibility)

	private String metricType; // BALANCE, PRICE, SUPPLY, NAV, TREASURY_BALANCE, HOT_WALLET_BALANCE (optional, inferred from alarmType if not provided)

	private String description;

	/**
	 * Get threshold value for service layer
	 */
	public BigDecimal getThresholdValue() {
		return threshold;
	}

	/**
	 * Get enabled status for service layer
	 */
	public Boolean getIsEnabled() {
		return isActive;
	}
}

