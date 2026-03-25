package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Mining Session Response DTO with detailed speed breakdown
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiningSessionResponse {
	private Long sessionId;
	private Long userId;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime startTime;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime endTime;
	private Long apEarned;
	private Double miningSpeed;
	private Boolean isActive;

	// Detailed speed breakdown fields
	private BigDecimal baseSpeed;
	private BigDecimal friendBonusSpeed;
	private BigDecimal bookingBonusSpeed;
	private BigDecimal videoBoostSpeed;
	private BigDecimal statusBonusSpeed;
	private BigDecimal taskBonusSpeed;
	private BigDecimal streakBonusSpeed;
	private BigDecimal stakingBonusSpeed;
	private BigDecimal activeMiningBonusSpeed;
	private Boolean isCapped;
}

