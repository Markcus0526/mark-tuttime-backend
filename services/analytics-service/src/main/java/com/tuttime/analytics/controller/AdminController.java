package com.tuttime.analytics.controller;

import com.tuttime.analytics.dto.AdminDto;
import com.tuttime.analytics.service.AnalyticsService;
import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics/admin")
@Tag(name = "1.Admin Analytics", description = "Global platform metrics and user risk profiling")
@RequiredArgsConstructor
public class AdminController {

	private final AnalyticsService analyticsService;

	@GetMapping("/platform-summary")
	@Operation(summary = "Get global GMV and TutTime commission totals")
	public ResponseEntity<ApiRes<AdminDto.GlobalMetricsResponse>> getPlatformSummary() {

		validateAdmin();

		AdminDto.GlobalMetricsResponse metrics = analyticsService.getPlatformSummary();
		return ResponseEntity.ok(ApiRes.success(metrics));
	}

	@GetMapping("/users/{userId}/risk-profile")
	@Operation(summary = "Check user reliability score and no-show history")
	public ResponseEntity<ApiRes<AdminDto.UserReliabilityResponse>> getUserRiskProfile(@PathVariable UUID userId) {

		validateAdmin();

		AdminDto.UserReliabilityResponse profile = analyticsService.getUserRiskProfile(userId);
		return ResponseEntity.ok(ApiRes.success(profile));
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
