package com.tuttime.restaurant.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.restaurant.dto.ScheduleDto;
import com.tuttime.restaurant.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/restaurant-schedules")
@Tag(name = "5.Restaurant Schedules", description = "Operations related to schedule of restaurants, branches")
public class ScheduleController {

	private final ScheduleService scheduleService;

	// --- Restaurant Schedules ---

	@Operation(summary = "Get schedules for a restaurant or branch", description = "Retrieve all schedule entries for a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Schedules retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@GetMapping("/{entityId}")
	public ResponseEntity<ApiRes<List<ScheduleDto.ScheduleResponse>>> getSchedules(
			@Parameter(description = "UUID of the restaurant or branch", required = true)
			@PathVariable UUID entityId) {

		List<ScheduleDto.ScheduleResponse> schedules = scheduleService.getSchedules(entityId);
		return ResponseEntity.ok(ApiRes.success(schedules));
	}

	@Operation(summary = "Add a schedule to a restaurant or branch", description = "Create a new schedule entry for a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Schedule created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid schedule data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@PostMapping
	public ResponseEntity<ApiRes<ScheduleDto.ScheduleResponse>> addSchedule(
			@Parameter(description = "Schedule details to create", required = true)
			@Valid @RequestBody ScheduleDto.ScheduleRequest request) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		ScheduleDto.ScheduleResponse created = scheduleService.createSchedule(request, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(created));
	}

	@Operation(summary = "Update a schedule", description = "Update a specific schedule entry for a restaurant or branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Schedule updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid schedule data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Schedule or restaurant not found")
	})
	@PutMapping("/{scheduleId}")
	public ResponseEntity<ApiRes<ScheduleDto.ScheduleResponse>> updateSchedule(
			@Parameter(description = "UUID of the schedule to update", required = true)
			@PathVariable UUID scheduleId,
			@Parameter(description = "Updated schedule details", required = true)
			@Valid @RequestBody ScheduleDto.ScheduleRequest request) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		ScheduleDto.ScheduleResponse updated = scheduleService.updateSchedule(scheduleId, request, currentUserId, userRoles);
		return ResponseEntity.ok(ApiRes.success(updated));
	}

	@Operation(summary = "Delete a schedule", description = "Delete a specific schedule entry from a restaurant or branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Schedule deleted successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Schedule or restaurant not found")
	})
	@DeleteMapping("/{scheduleId}")
	public ResponseEntity<ApiRes<Void>> deleteSchedule(
			@Parameter(description = "UUID of the schedule to delete", required = true)
			@PathVariable UUID scheduleId) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		scheduleService.deleteSchedule(scheduleId, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
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
			throw new ForbiddenException("Access forbidden: You do not manage this method");
		}
	}

	private void validateAdminOrOwner() {
		if (!UserContext.isValidUUID()
				|| !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			throw new ForbiddenException("Access forbidden: You do not manage this method");
		}
	}

	private void validateAdminOrOwnerOrManager() {
		if (!UserContext.isValidUUID()
				|| !(UserContext.isRoleAdmin() || UserContext.isRoleOwner() || UserContext.isRoleManager())) {
			throw new ForbiddenException("Access forbidden: You do not manage this method");
		}
	}

}