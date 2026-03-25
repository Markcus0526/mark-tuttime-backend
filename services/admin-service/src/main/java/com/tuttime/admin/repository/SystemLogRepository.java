package com.tuttime.admin.repository;

import com.tuttime.admin.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repository for System Log entities
 */
@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

	/**
	 * Find logs by service name
	 */
	Page<SystemLog> findByServiceName(String serviceName, Pageable pageable);

	/**
	 * Find logs by log level
	 */
	Page<SystemLog> findByLogLevel(SystemLog.LogLevel logLevel, Pageable pageable);

	/**
	 * Find logs by service and level
	 */
	Page<SystemLog> findByServiceNameAndLogLevel(String serviceName, SystemLog.LogLevel logLevel, Pageable pageable);

	/**
	 * Find logs by service, level, and date range
	 */
	@Query("SELECT l FROM SystemLog l WHERE " +
			"(:serviceName IS NULL OR l.serviceName = :serviceName) AND " +
			"(:logLevel IS NULL OR l.logLevel = :logLevel) AND " +
			"(:startDate IS NULL OR l.createdAt >= :startDate) AND " +
			"(:endDate IS NULL OR l.createdAt <= :endDate) " +
			"ORDER BY l.createdAt DESC")
	Page<SystemLog> findLogsWithFilters(
			@Param("serviceName") String serviceName,
			@Param("logLevel") SystemLog.LogLevel logLevel,
			@Param("startDate") OffsetDateTime startDate,
			@Param("endDate") OffsetDateTime endDate,
			Pageable pageable);

	/**
	 * Find error logs only (ERROR and FATAL)
	 */
	@Query("SELECT l FROM SystemLog l WHERE " +
			"l.logLevel IN ('ERROR', 'FATAL') AND " +
			"(:serviceName IS NULL OR l.serviceName = :serviceName) AND " +
			"(:startDate IS NULL OR l.createdAt >= :startDate) AND " +
			"(:endDate IS NULL OR l.createdAt <= :endDate) " +
			"ORDER BY l.createdAt DESC")
	Page<SystemLog> findErrorLogs(
			@Param("serviceName") String serviceName,
			@Param("startDate") OffsetDateTime startDate,
			@Param("endDate") OffsetDateTime endDate,
			Pageable pageable);

	/**
	 * Find warning logs only
	 */
	@Query("SELECT l FROM SystemLog l WHERE " +
			"l.logLevel = 'WARN' AND " +
			"(:serviceName IS NULL OR l.serviceName = :serviceName) AND " +
			"(:startDate IS NULL OR l.createdAt >= :startDate) AND " +
			"(:endDate IS NULL OR l.createdAt <= :endDate) " +
			"ORDER BY l.createdAt DESC")
	Page<SystemLog> findWarningLogs(
			@Param("serviceName") String serviceName,
			@Param("startDate") OffsetDateTime startDate,
			@Param("endDate") OffsetDateTime endDate,
			Pageable pageable);

	/**
	 * Get distinct service names
	 */
	@Query("SELECT DISTINCT l.serviceName FROM SystemLog l ORDER BY l.serviceName")
	List<String> findDistinctServiceNames();

	/**
	 * Find logs by trace ID (for distributed tracing)
	 */
	Page<SystemLog> findByTraceId(String traceId, Pageable pageable);
}

