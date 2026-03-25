package com.tuttime.admin.service;

import com.tuttime.admin.dto.SystemLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Admin Log Service Interface
 */
public interface AdminLogService {

	/**
	 * Get system logs with filters
	 */
	Page<SystemLogResponse> getLogs(String service, String level,
	                                OffsetDateTime startDate, OffsetDateTime endDate,
	                                Pageable pageable);

	/**
	 * Get specific log entry by ID
	 */
	SystemLogResponse getLogById(Long logId);

	/**
	 * List all services that have logs
	 */
	List<String> getAllServices();

	/**
	 * Get error logs only (ERROR and FATAL levels)
	 */
	Page<SystemLogResponse> getErrorLogs(String service,
	                                     OffsetDateTime startDate, OffsetDateTime endDate,
	                                     Pageable pageable);

	/**
	 * Get warning logs only
	 */
	Page<SystemLogResponse> getWarningLogs(String service,
	                                       OffsetDateTime startDate, OffsetDateTime endDate,
	                                       Pageable pageable);
}

