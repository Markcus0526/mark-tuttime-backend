package com.tuttime.analytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tuttime.analytics.dto.AdminDto;
import com.tuttime.analytics.entity.BranchPerformanceStat;
import com.tuttime.analytics.entity.BranchYieldManagement;
import com.tuttime.analytics.entity.FinancialLedger;
import com.tuttime.analytics.entity.UserReliabilityIndex;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AnalyticsService {

	List<BranchYieldManagement> getWeeklyHeatmap(UUID branchId);

	FinancialLedger getDailyFinancials(UUID branchId, LocalDate date);

	AdminDto.GlobalMetricsDto calculateGlobalMetrics();

	BranchPerformanceStat getBranchStats(UUID branchId);

	void processEvent(String source, String type, UUID branchId, Object payload);

	// Used by gRPC Provider
	UserReliabilityIndex getUserReliability(UUID userId);

}