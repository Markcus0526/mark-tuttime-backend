package com.tuttime.admin.service;

import com.tuttime.admin.dto.WarningCategory;
import com.tuttime.admin.dto.WarningResponse;
import com.tuttime.admin.dto.WarningSeverity;
import com.tuttime.admin.dto.WarningStatisticsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;

/**
 * Admin Warning Service Interface
 *
 * <p>Provides consolidated warnings view from various sources:
 * <ul>
 *   <li>Fraud flags</li>
 *   <li>Brute-force attempts</li>
 *   <li>System stability warnings</li>
 * </ul>
 */
public interface AdminWarningService {

	/**
	 * Get consolidated warnings from all sources
	 *
	 * @param pageable  Pagination parameters
	 * @param category  Optional category filter
	 * @param severity  Optional severity filter
	 * @param resolved  Optional resolved status filter
	 * @param startDate Optional start date filter
	 * @param endDate   Optional end date filter
	 * @return Page of warnings
	 */
	Page<WarningResponse> getConsolidatedWarnings(
			Pageable pageable,
			WarningCategory category,
			WarningSeverity severity,
			Boolean resolved,
			OffsetDateTime startDate,
			OffsetDateTime endDate
	);

	/**
	 * Get brute-force attempt warnings
	 *
	 * @param pageable  Pagination parameters
	 * @param startDate Optional start date filter
	 * @param endDate   Optional end date filter
	 * @return Page of brute-force warnings
	 */
	Page<WarningResponse> getBruteForceWarnings(
			Pageable pageable,
			OffsetDateTime startDate,
			OffsetDateTime endDate
	);

	/**
	 * Get system stability warnings
	 *
	 * @param pageable  Pagination parameters
	 * @param startDate Optional start date filter
	 * @param endDate   Optional end date filter
	 * @return Page of system stability warnings
	 */
	Page<WarningResponse> getSystemStabilityWarnings(
			Pageable pageable,
			OffsetDateTime startDate,
			OffsetDateTime endDate
	);

	/**
	 * Mark a warning as resolved
	 *
	 * @param warningId Warning ID
	 */
	void resolveWarning(String warningId);

	/**
	 * Get warning statistics
	 *
	 * @return Warning statistics by category and severity
	 */
	WarningStatisticsResponse getWarningStatistics();
}

