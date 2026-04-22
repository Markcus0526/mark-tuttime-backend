package com.tuttime.analytics.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class RestaurantDto {

	@Builder
	public record YieldHeatmapResponse(
			UUID branchId,
			OffsetDateTime timeSlot,
			Integer currentBookings,
			BigDecimal occupancyRate,
			String demandLevel,
			Float recommendedDiscountPct
	) {}

	@Builder
	public record DailyLedgerResponse(
			UUID branchId,
			LocalDate reportDate,
			BigDecimal totalGmv,
			BigDecimal commissionEarned,
			BigDecimal payoutDueRestaurant,
			BigDecimal refundVolume
	) {}

	public record BranchPerformanceResponse(
			UUID branchId,
			Integer avgPrepTimeSec,
			BigDecimal posSyncHealthPct,
			BigDecimal avgGuestRating,
			UUID popularDishId
	) {}
}
