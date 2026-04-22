package com.tuttime.analytics.dto;

import lombok.Builder;

import java.math.BigDecimal;

public class AdminDto {

	@Builder
	public record GlobalMetricsDto(
			BigDecimal totalGmv,             // Total guest spending (Gross Merchandise Value)
			BigDecimal totalCommission,      // TutTime's actual revenue (The 150-240M target)
			BigDecimal totalRefunds,         // Impact of cancellations on cash flow
			long activeBranchesCount,        // Scale tracking (towards the 1,000 goal)
			long totalBookingsCount,         // Platform usage volume
			double averageCommissionRate,    // Health of the 5–12% margin
			double platformYieldPercentage   // Efficiency of filling "dead hours"
	) {}
}
