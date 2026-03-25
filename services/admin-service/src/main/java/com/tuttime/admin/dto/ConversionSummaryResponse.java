package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Conversion Summary Response DTO
 *
 * <p>Contains summary statistics for the last conversion event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionSummaryResponse {
	private Long totalUsers;  // Total users who participated (mapped from totalUsersCount)
	private BigDecimal totalApAmount;  // Total AP Credits converted
	private BigDecimal averageUserShare;  // Average user share percentage
	private BigDecimal averageDistributedApartAmount;  // Average APART tokens distributed per user (mapped from averageDistributedApart)

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime conversionDate;  // When the conversion occurred (optional, not in required format)

	private Long conversionId;  // Conversion event ID (optional, not in required format)
}

