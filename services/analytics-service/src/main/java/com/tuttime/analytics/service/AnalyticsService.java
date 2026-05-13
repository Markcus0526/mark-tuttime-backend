package com.tuttime.analytics.service;

import com.tuttime.analytics.dto.AdminDto;
import com.tuttime.analytics.dto.RestaurantDto;
import com.tuttime.analytics.entity.UserReliabilityIndex;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AnalyticsService {

	// Admin Analytics
	AdminDto.GlobalMetricsResponse getPlatformSummary();

	AdminDto.UserReliabilityResponse getUserRiskProfile(UUID userId);

	// Restaurant Analytics
	RestaurantDto.OccupancyResponse getRealtimeOccupancy(UUID branchId, UUID userId, String roles);

	List<RestaurantDto.TopItemResponse> getTopItems(UUID branchId, LocalDate from, LocalDate to, int limit,
	                                                UUID userId, String roles);

	RestaurantDto.RevenueBreakdownResponse getRevenueBreakdown(UUID branchId, String granularity,
	                                                           LocalDate date, UUID userId, String roles);

	RestaurantDto.RetentionResponse getCustomerRetention(UUID branchId, String period, UUID userId, String roles);

	RestaurantDto.PosHealthResponse getPosHealth(UUID branchId, UUID userId, String roles);

	RestaurantDto.FunnelResponse getConversionFunnel(UUID branchId, LocalDate startDate, LocalDate endDate,
	                                                 UUID userId, String roles);

	RestaurantDto.AovTrendResponse getAovTrend(UUID branchId, String granularity, int limit,
	                                           UUID userId, String roles);

	List<RestaurantDto.TopCustomerResponse> getTopCustomers(UUID branchId, int limit, UUID userId, String roles);

	RestaurantDto.MenuPerformanceResponse getMenuPerformance(UUID branchId, LocalDate startDate, LocalDate endDate,
	                                                         String groupBy, UUID userId, String roles);

	// Used by Kafka Consumer
	void processEvent(String source, String type, UUID branchId, Object payload);

	// Used by gRPC Provider
	UserReliabilityIndex getUserReliability(UUID userId);

}