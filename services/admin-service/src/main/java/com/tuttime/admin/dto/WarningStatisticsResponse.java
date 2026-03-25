package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Warning Statistics Response DTO
 *
 * <p>Provides aggregated statistics about warnings by category and severity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarningStatisticsResponse {

	private Long totalWarnings;
	private Long unresolvedWarnings;

	// Map of category name (String) to count
	private Map<String, Long> byCategory;

	// Map of severity name (String) to count
	private Map<String, Long> bySeverity;
}

