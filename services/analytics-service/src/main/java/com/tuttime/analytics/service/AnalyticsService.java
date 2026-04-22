package com.tuttime.analytics.service;

import com.tuttime.analytics.dto.AdminDto;
import com.tuttime.analytics.dto.RestaurantDto;
import com.tuttime.analytics.entity.BranchPerformanceStat;
import com.tuttime.analytics.entity.BranchYieldManagement;
import com.tuttime.analytics.entity.FinancialLedger;
import com.tuttime.analytics.entity.UserReliabilityIndex;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AnalyticsService {

	// Admin Analytics
	AdminDto.GlobalMetricsResponse getPlatformSummary();

	AdminDto.UserReliabilityResponse getUserRiskProfile(UUID userId);

	// Restaurant Analytics
	List<RestaurantDto.YieldHeatmapResponse> getWeeklyHeatmap(UUID branchId);

	RestaurantDto.DailyLedgerResponse getDailyFinancials(UUID branchId, LocalDate date);

	RestaurantDto.BranchPerformanceResponse getBranchStats(UUID branchId);

	// Used by Kafka Consumer
	void processEvent(String source, String type, UUID branchId, Object payload);

	// Used by gRPC Provider
	UserReliabilityIndex getUserReliability(UUID userId);

}