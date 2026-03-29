package com.tuttime.restaurant.controller;

import java.util.List;
import java.util.UUID;

import com.tuttime.common.dto.ApiRes;
import com.tuttime.restaurant.dto.ScheduleRequest;
import com.tuttime.restaurant.dto.ScheduleResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Schedules", description = "Operations related to restaurant and branch schedules")
public class ScheduleController {

	private final ScheduleService scheduleService;

	// --- Restaurant Schedules ---

	@Operation(summary = "Get schedules for a restaurant", description = "Retrieve all schedule entries for a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Schedules retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@GetMapping("/restaurants/{restaurantId}/schedules")
	@PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'MANAGER', 'HOSTESS')")
	public ResponseEntity<ApiRes<List<ScheduleResponse>>> getRestaurantSchedules(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId) {
		List<ScheduleResponse> schedules = scheduleService.getSchedulesByRestaurantId(restaurantId);
		return ResponseEntity.ok(ApiRes.success(schedules));
	}

	@Operation(summary = "Add a schedule to a restaurant", description = "Create a new schedule entry for a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Schedule created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid schedule data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@PostMapping("/restaurants/{restaurantId}/schedules")
	@PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(#restaurantId, @securityUtil.getCurrentUserId()) or hasRole('MANAGER')")
	public ResponseEntity<ApiRes<ScheduleResponse>> addRestaurantSchedule(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "Schedule details to create", required = true)
			@Valid @RequestBody ScheduleRequest request) {
		ScheduleResponse created = scheduleService.createScheduleForRestaurant(restaurantId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(created));
	}

	@Operation(summary = "Update a restaurant schedule", description = "Update a specific schedule entry for a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Schedule updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid schedule data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Schedule or restaurant not found")
	})
	@PutMapping("/restaurants/{restaurantId}/schedules/{scheduleId}")
	@PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(#restaurantId, @securityUtil.getCurrentUserId()) or hasRole('MANAGER')")
	public ResponseEntity<ApiRes<ScheduleResponse>> updateRestaurantSchedule(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the schedule to update", required = true)
			@PathVariable UUID scheduleId,
			@Parameter(description = "Updated schedule details", required = true)
			@Valid @RequestBody ScheduleRequest request) {
		ScheduleResponse updated = scheduleService.updateSchedule(scheduleId, restaurantId, null, request);
		return ResponseEntity.ok(ApiRes.success(updated));
	}

	@Operation(summary = "Delete a restaurant schedule", description = "Delete a specific schedule entry from a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Schedule deleted successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Schedule or restaurant not found")
	})
	@DeleteMapping("/restaurants/{restaurantId}/schedules/{scheduleId}")
	@PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(#restaurantId, @securityUtil.getCurrentUserId()) or hasRole('MANAGER')")
	public ResponseEntity<Void> deleteRestaurantSchedule(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the schedule to delete", required = true)
			@PathVariable UUID scheduleId) {
		scheduleService.deleteSchedule(scheduleId, restaurantId, null);
		return ResponseEntity.noContent().build();
	}

	// --- Branch Schedules ---

	@Operation(summary = "Get schedules for a branch", description = "Retrieve all schedule entries for a branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Schedules retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Branch not found")
	})
	@GetMapping("/branches/{branchId}/schedules")
	@PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'MANAGER', 'HOSTESS')")
	public ResponseEntity<ApiRes<List<ScheduleResponse>>> getBranchSchedules(
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId) {
		List<ScheduleResponse> schedules = scheduleService.getSchedulesByBranchId(branchId);
		return ResponseEntity.ok(ApiRes.success(schedules));
	}

	@Operation(summary = "Add a schedule to a branch", description = "Create a new schedule entry for a branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Schedule created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid schedule data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Branch not found")
	})
	@PostMapping("/branches/{branchId}/schedules")
	@PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(#restaurantId, @securityUtil.getCurrentUserId()) or hasRole('MANAGER')")
	public ResponseEntity<ApiRes<ScheduleResponse>> addBranchSchedule(
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId,
			@Parameter(description = "Schedule details to create", required = true)
			@Valid @RequestBody ScheduleRequest request) {
		ScheduleResponse created = scheduleService.createScheduleForBranch(branchId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(created));
	}

	@Operation(summary = "Update a branch schedule", description = "Update a specific schedule entry for a branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Schedule updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid schedule data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Schedule or branch not found")
	})
	@PutMapping("/branches/{branchId}/schedules/{scheduleId}")
	@PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(#restaurantId, @securityUtil.getCurrentUserId()) or hasRole('MANAGER')")
	public ResponseEntity<ApiRes<ScheduleResponse>> updateBranchSchedule(
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId,
			@Parameter(description = "UUID of the schedule to update", required = true)
			@PathVariable UUID scheduleId,
			@Parameter(description = "Updated schedule details", required = true)
			@Valid @RequestBody ScheduleRequest request) {
		ScheduleResponse updated = scheduleService.updateSchedule(scheduleId, null, branchId, request);
		return ResponseEntity.ok(ApiRes.success(updated));
	}

	@Operation(summary = "Delete a branch schedule", description = "Delete a specific schedule entry from a branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Schedule deleted successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Schedule or branch not found")
	})
	@DeleteMapping("/branches/{branchId}/schedules/{scheduleId}")
	@PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(#restaurantId, @securityUtil.getCurrentUserId()) or hasRole('MANAGER')")
	public ResponseEntity<Void> deleteBranchSchedule(
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId,
			@Parameter(description = "UUID of the schedule to delete", required = true)
			@PathVariable UUID scheduleId) {
		scheduleService.deleteSchedule(scheduleId, null, branchId);
		return ResponseEntity.noContent().build();
	}
}
