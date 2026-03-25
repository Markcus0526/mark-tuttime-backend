package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Response DTO for vesting pool details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VestingPoolResponse {

	private Long poolId;
	private String poolName;
	private String description;
	private String contractAddress;
	private BigDecimal totalAllocation;
	private BigDecimal currentBalance;
	private BigDecimal claimedAmount; // Total amount claimed by all investors
	private BigDecimal remainingBalance; // Remaining unclaimed tokens
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime startDate;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime endDate;
	private Integer vestingPeriodMonths;
	private Integer cliffPeriodMonths;
	private Boolean isActive;
	private Integer investorCount; // Number of investors in pool
	private String metadata;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime updatedAt;
}

