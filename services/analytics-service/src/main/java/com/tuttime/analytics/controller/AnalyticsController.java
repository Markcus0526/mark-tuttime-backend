package com.tuttime.analytics.controller;

import com.tuttime.analytics.dto.EventResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics Management", description = "Endpoints for retrieving system-wide event logs")
public class AnalyticsController {

	private final AnalyticsService analyticsService;

	@GetMapping("/history/{restaurantId}")
	@Operation(summary = "Get restaurant history", description = "Retrieves all logged events for a specific restaurant")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "History retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	public ResponseEntity<ApiRes<List<EventResponse>>> getHistory(@PathVariable UUID restaurantId) {
		// Example: Tracking which admin is requesting the data
		String requesterId = UserContext.getUserId();

		List<EventResponse> response = analyticsService.getRestaurantHistory(restaurantId);
		return ResponseEntity.ok(ApiRes.success(response));
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
