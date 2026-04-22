package com.tuttime.analytics.mapper;

import com.tuttime.analytics.dto.RestaurantDto;
import com.tuttime.analytics.entity.BranchPerformanceStat;
import com.tuttime.analytics.entity.BranchYieldManagement;
import com.tuttime.analytics.entity.FinancialLedger;
import org.mapstruct.Mapper;

import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RestaurantMapper {

	// 1. Heatmap Mapping (handling the Composite ID)
	@Mapping(target = "branchId", source = "id.branchId")
	@Mapping(target = "timeSlot", source = "id.timeSlot")
	RestaurantDto.YieldHeatmapResponse toHeatmapResponse(BranchYieldManagement entity);

	List<RestaurantDto.YieldHeatmapResponse> toHeatmapListResponse(List<BranchYieldManagement> entities);

	// 2. Ledger Mapping
	RestaurantDto.DailyLedgerResponse toLedgerResponse(FinancialLedger entity);

	// 3. Performance Mapping
	RestaurantDto.BranchPerformanceResponse toPerformanceResponse(BranchPerformanceStat entity);
}