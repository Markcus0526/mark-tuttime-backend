package com.tuttime.admin.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Reserve Alarm Entity
 *
 * <p>Represents alarms for token reserves monitoring, such as low balance,
 * price drops, supply thresholds, etc.
 */
@Entity
@Table(name = "reserve_alarms", schema = "admin_service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ReserveAlarm {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "alarm_id")
	private Long alarmId;

	@Column(name = "network", length = 50)
	private String network; // null means all networks

	@Enumerated(EnumType.STRING)
	@Column(name = "alarm_type", nullable = false, length = 50)
	private AlarmType alarmType;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	@Builder.Default
	private AlarmStatus status = AlarmStatus.ACTIVE;

	@Column(name = "threshold_value", nullable = false, precision = 36, scale = 18)
	private BigDecimal thresholdValue;

	@Column(name = "comparison_operator", nullable = false, length = 10)
	@Builder.Default
	private String comparisonOperator = "LT"; // LT, GT, EQ, LTE, GTE

	@Column(name = "metric_type", nullable = false, length = 50)
	private String metricType; // BALANCE, PRICE, SUPPLY, NAV, TREASURY_BALANCE, HOT_WALLET_BALANCE

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "triggered_at")
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime triggeredAt;

	@Column(name = "resolved_at")
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime resolvedAt;

	@Column(name = "is_enabled", nullable = false)
	@Builder.Default
	private Boolean isEnabled = true;

	@Column(name = "created_by")
	private Long createdBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	@CreatedDate
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	@LastModifiedDate
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime updatedAt;

	/**
	 * Alarm Types
	 */
	public enum AlarmType {
		LOW_BALANCE,
		PRICE_DROP,
		PRICE_INCREASE,
		HIGH_VOLATILITY,
		SUPPLY_THRESHOLD,
		NAV_THRESHOLD
	}

	/**
	 * Alarm Status
	 */
	public enum AlarmStatus {
		ACTIVE,
		TRIGGERED,
		RESOLVED,
		DISABLED
	}

	/**
	 * Check if alarm should trigger based on current value
	 */
	public boolean shouldTrigger(BigDecimal currentValue) {
		if (!isEnabled || status != AlarmStatus.ACTIVE) {
			return false;
		}

		int comparison = currentValue.compareTo(thresholdValue);

		return switch (comparisonOperator) {
			case "LT" -> comparison < 0;
			case "GT" -> comparison > 0;
			case "EQ" -> comparison == 0;
			case "LTE" -> comparison <= 0;
			case "GTE" -> comparison >= 0;
			default -> false;
		};
	}

	/**
	 * Mark alarm as triggered
	 */
	public void trigger() {
		this.status = AlarmStatus.TRIGGERED;
		this.triggeredAt = OffsetDateTime.now(ZoneOffset.UTC);
	}

	/**
	 * Mark alarm as resolved
	 */
	public void resolve() {
		this.status = AlarmStatus.RESOLVED;
		this.resolvedAt = OffsetDateTime.now(ZoneOffset.UTC);
	}

	/**
	 * Reset alarm to active state
	 */
	public void reset() {
		this.status = AlarmStatus.ACTIVE;
		this.triggeredAt = null;
		this.resolvedAt = null;
	}
}

