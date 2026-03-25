package com.tuttime.admin.service;

import com.tuttime.admin.dto.BruteForceHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;

/**
 * Admin Brute-Force History Service Interface
 */
public interface AdminBruteForceHistoryService {

	/**
	 * Get brute-force history
	 *
	 * @param pageable    Pagination parameters
	 * @param startDate   Optional start date filter
	 * @param endDate     Optional end date filter
	 * @param ipAddress   Optional IP address filter
	 * @param phoneNumber Optional phone number filter
	 * @return Page of brute-force history entries
	 */
	Page<BruteForceHistoryResponse> getBruteForceHistory(
			Pageable pageable,
			OffsetDateTime startDate,
			OffsetDateTime endDate,
			String ipAddress,
			String phoneNumber
	);
}

