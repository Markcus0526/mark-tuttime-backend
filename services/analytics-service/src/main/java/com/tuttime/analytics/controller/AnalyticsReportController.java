package com.tuttime.analytics.controller;

import com.tuttime.analytics.dto.HourlyBookingProjection;
import com.tuttime.analytics.dto.TopRestaurantProjection;
import com.tuttime.analytics.service.AnalyticsService;
import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics/reports")
@RequiredArgsConstructor
@Tag(name = "Analytics Reports", description = "Business intelligence and heatmap data")
public class AnalyticsReportController {

	private final AnalyticsService analyticsService;

	@GetMapping("/top-restaurants")
	@Operation(summary = "Get top 10 restaurants", description = "Ranking based on booking volume over a set period")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Report generated successfully")
	})
	public ResponseEntity<ApiRes<List<TopRestaurantProjection>>> getTopTen(
			@RequestParam(defaultValue = "7") int days) {

		List<TopRestaurantProjection> report = analyticsService.getTopRestaurants(days);
		return ResponseEntity.ok(ApiRes.success(report));
	}

	@GetMapping("/booking-heatmap")
	@Operation(summary = "Get booking heatmap", description = "Distribution of bookings per hour (0-23)")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Heatmap generated successfully")
	})
	public ResponseEntity<ApiRes<List<HourlyBookingProjection>>> getHeatmap(
			@RequestParam(defaultValue = "30") int days) {

		List<HourlyBookingProjection> data = analyticsService.getBookingHeatmap(days);
		return ResponseEntity.ok(ApiRes.success(data));
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
