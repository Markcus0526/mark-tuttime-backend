package com.tuttime.analytics.controller;

import com.tuttime.analytics.dto.RestaurantDto;
import com.tuttime.analytics.service.AnalyticsService;
import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics/restaurant")
@Tag(name = "2.Restaurant Analytics", description = "Endpoints for restaurant owners to monitor branch performance")
@RequiredArgsConstructor
public class RestaurantController {

	private final AnalyticsService analyticsService;

	@GetMapping("/{branchId}/heatmap")
	@Operation(summary = "Get 15-minute occupancy heatmap for yield management")
	public ResponseEntity<ApiRes<List<RestaurantDto.YieldHeatmapResponse>>> getHeatmap(@PathVariable UUID branchId) {

		validateAdminOrOwner();

		List<RestaurantDto.YieldHeatmapResponse> data = analyticsService.getWeeklyHeatmap(branchId);
		return ResponseEntity.ok(ApiRes.success(data));
	}

	@GetMapping("/{branchId}/daily-ledger")
	@Operation(summary = "Get financial summary (GMV, Commission, Payouts) for a specific date")
	public ResponseEntity<ApiRes<RestaurantDto.DailyLedgerResponse>> getDailyLedger(
			@PathVariable UUID branchId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

		validateAdminOrOwner();

		RestaurantDto.DailyLedgerResponse ledger = analyticsService.getDailyFinancials(branchId, date);
		return ResponseEntity.ok(ApiRes.success(ledger));
	}

	@GetMapping("/{branchId}/performance")
	@Operation(summary = "Get kitchen speed and POS sync health metrics")
	public ResponseEntity<ApiRes<RestaurantDto.BranchPerformanceResponse>> getPerformance(@PathVariable UUID branchId) {

		validateAdminOrOwner();

		RestaurantDto.BranchPerformanceResponse stats = analyticsService.getBranchStats(branchId);
		return ResponseEntity.ok(ApiRes.success(stats));
	}

	/**
	 * Private helper to keep the controller DRY
	 */
	private void validateLogin() {
		if (!UserContext.isValidUUID()) {
			throw new UnauthorizedException("Unauthorized: You should login");
		}
	}

	private void validateAdmin() {
		if (!UserContext.isValidUUID() || !UserContext.isRoleAdmin()) {
			throw new ForbiddenException("Access forbidden: Insufficient permissions");
		}
	}

	private void validateAdminOrOwner() {
		if (!UserContext.isValidUUID()
				|| !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			throw new ForbiddenException("Access forbidden: Insufficient permissions");
		}
	}

	private void validateAdminOrOwnerOrManager() {
		if (!UserContext.isValidUUID()
				|| !(UserContext.isRoleAdmin() || UserContext.isRoleOwner() || UserContext.isRoleManager())) {
			throw new ForbiddenException("Access forbidden: Insufficient permissions");
		}
	}
}
