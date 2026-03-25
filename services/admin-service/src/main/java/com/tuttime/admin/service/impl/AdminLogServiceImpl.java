package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.SystemLogResponse;
import com.tuttime.admin.entity.SystemLog;
import com.tuttime.admin.repository.SystemLogRepository;
import com.tuttime.admin.service.AdminLogService;
import com.tuttime.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Admin Log Service Implementation
 *
 * <p>Manages system logs from all microservices. This service provides
 * functionality to view, filter, and search logs from across the system.
 *
 * <p><b>Integration Notes:</b>
 * <ul>
 *   <li>Currently reads from database table (system_logs)</li>
 *   <li>Can be extended to integrate with centralized logging systems:
 *       <ul>
 *         <li>ELK Stack (Elasticsearch, Logstash, Kibana)</li>
 *         <li>AWS CloudWatch Logs</li>
 *         <li>Google Cloud Logging</li>
 *         <li>Azure Monitor</li>
 *         <li>Other log aggregation services</li>
 *       </ul>
 *   </li>
 *   <li>Logs should be collected from services via:
 *       <ul>
 *         <li>Log file parsing (Filebeat, Fluentd)</li>
 *         <li>Direct API integration</li>
 *         <li>Message queue (Kafka, RabbitMQ)</li>
 *       </ul>
 *   </li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLogServiceImpl implements AdminLogService {

	private final SystemLogRepository systemLogRepository;

	@Override
	@Transactional(readOnly = true)
	public Page<SystemLogResponse> getLogs(String service, String level,
	                                       OffsetDateTime startDate, OffsetDateTime endDate,
	                                       Pageable pageable) {
		log.debug("Fetching logs: service={}, level={}, startDate={}, endDate={}",
				service, level, startDate, endDate);

		SystemLog.LogLevel logLevel = null;
		if (level != null && !level.isEmpty()) {
			try {
				logLevel = SystemLog.LogLevel.valueOf(level.toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new BusinessException("Invalid log level: " + level);
			}
		}

		Page<SystemLog> logs = systemLogRepository.findLogsWithFilters(
				service, logLevel, startDate, endDate, pageable);

		return logs.map(this::toResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public SystemLogResponse getLogById(Long logId) {
		log.debug("Fetching log by ID: {}", logId);

		SystemLog log = systemLogRepository.findById(logId)
				.orElseThrow(() -> new BusinessException("Log not found: " + logId));

		return toResponse(log);
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> getAllServices() {
		log.debug("Fetching all service names");

		List<String> services = systemLogRepository.findDistinctServiceNames();

		// If no logs in database, return default service list
		if (services.isEmpty()) {
			return List.of(
					"admin-service",
					"user-service",
					"token-service",
					"mining-service",
					"staking-service",
					"rewards-service",
					"conversion-service",
					"content-service",
					"notification-service",
					"booking-integration-service"
			);
		}

		return services;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<SystemLogResponse> getErrorLogs(String service,
	                                            OffsetDateTime startDate, OffsetDateTime endDate,
	                                            Pageable pageable) {
		log.debug("Fetching error logs: service={}, startDate={}, endDate={}",
				service, startDate, endDate);

		Page<SystemLog> logs = systemLogRepository.findErrorLogs(
				service, startDate, endDate, pageable);

		return logs.map(this::toResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<SystemLogResponse> getWarningLogs(String service,
	                                              OffsetDateTime startDate, OffsetDateTime endDate,
	                                              Pageable pageable) {
		log.debug("Fetching warning logs: service={}, startDate={}, endDate={}",
				service, startDate, endDate);

		Page<SystemLog> logs = systemLogRepository.findWarningLogs(
				service, startDate, endDate, pageable);

		return logs.map(this::toResponse);
	}

	/**
	 * Convert entity to response DTO
	 */
	private SystemLogResponse toResponse(SystemLog log) {
		return SystemLogResponse.builder()
				.logId(log.getLogId())
				.serviceName(log.getServiceName())
				.logLevel(log.getLogLevel() != null ? log.getLogLevel().name() : null)
				.loggerName(log.getLoggerName())
				.message(log.getMessage())
				.stackTrace(log.getStackTrace())
				.threadName(log.getThreadName())
				.hostName(log.getHostName())
				.ipAddress(log.getIpAddress())
				.userId(log.getUserId())
				.requestId(log.getRequestId())
				.traceId(log.getTraceId())
				.spanId(log.getSpanId())
				.metadata(log.getMetadata())
				.createdAt(log.getCreatedAt())
				.build();
	}
}

