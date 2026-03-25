package com.tuttime.admin.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * System Log Entity
 *
 * <p>Represents log entries from all microservices in the system.
 * This table stores log entries that can be queried and filtered
 * by service, log level, date range, etc.
 */
@Entity
@Table(name = "system_logs", schema = "admin_service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SystemLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "log_id")
	private Long logId;

	@Column(name = "service_name", nullable = false, length = 100)
	private String serviceName;

	@Enumerated(EnumType.STRING)
	@Column(name = "log_level", nullable = false, length = 10)
	private LogLevel logLevel;

	@Column(name = "logger_name", length = 255)
	private String loggerName;

	@Column(name = "message", nullable = false, columnDefinition = "TEXT")
	private String message;

	@Column(name = "stack_trace", columnDefinition = "TEXT")
	private String stackTrace;

	@Column(name = "thread_name", length = 255)
	private String threadName;

	@Column(name = "host_name", length = 255)
	private String hostName;

	@Column(name = "ip_address", length = 45)
	private String ipAddress;

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "request_id", length = 100)
	private String requestId;

	@Column(name = "trace_id", length = 100)
	private String traceId;

	@Column(name = "span_id", length = 100)
	private String spanId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "metadata", columnDefinition = "jsonb")
	private Map<String, Object> metadata;

	@Column(name = "created_at", nullable = false, updatable = false)
	@CreatedDate
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;

	/**
	 * Log Levels
	 */
	public enum LogLevel {
		TRACE,
		DEBUG,
		INFO,
		WARN,
		ERROR,
		FATAL
	}

	/**
	 * Check if log is an error level
	 */
	public boolean isError() {
		return logLevel == LogLevel.ERROR || logLevel == LogLevel.FATAL;
	}

	/**
	 * Check if log is a warning level
	 */
	public boolean isWarning() {
		return logLevel == LogLevel.WARN;
	}
}

