package com.tuttime.admin.service;

import com.tuttime.admin.dto.SystemStabilityHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;

/**
 * Admin System Stability History Service Interface
 */
public interface AdminSystemStabilityHistoryService {

	/**
	 * Get system stability history
	 *
	 * @param pageable    Pagination parameters
	 * @param startDate   Optional start date filter
	 * @param endDate     Optional end date filter
	 * @param eventType   Optional event type filter
	 * @param serviceName Optional service name filter
	 * @return Page of system stability history entries
	 */
	Page<SystemStabilityHistoryResponse> getSystemStabilityHistory(
			Pageable pageable,
			OffsetDateTime startDate,
			OffsetDateTime endDate,
			String eventType,
			String serviceName
	);
}

