package com.tuttime.restaurant.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.restaurant.dto.BranchDto;
import com.tuttime.restaurant.dto.ZoneDto;
import com.tuttime.restaurant.service.*;
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
@RequestMapping("/api/v1/branches")
@Tag(name = "Branches", description = "Operations related to branches, schedules & media")
public class BranchController {

	private final BranchService branchService;
	private final RestaurantService restaurantService;
	private final ScheduleService scheduleService;
	private final MediaService mediaService;
	private final ZoneService zoneService;

	// --- Branch CRUD ---

	@Operation(summary = "Get a branch by ID", description = "Retrieve detailed information about a branch by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Branch retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Branch not found")
	})
	@GetMapping("/{branchId}")
	public ResponseEntity<ApiRes<BranchDto.BranchResponse>> getBranchById(
			@Parameter(description = "UUID of the branch to retrieve", required = true)
			@PathVariable UUID branchId) {

		BranchDto.BranchResponse branch = branchService.getBranchById(branchId);
		return ResponseEntity.ok(ApiRes.success(branch));
	}

	@Operation(summary = "Create a new branch", description = "Add a new branch to a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Branch created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input data"),
			@ApiResponse(responseCode = "403", description = "Access denied")
	})
	@PostMapping
	public ResponseEntity<ApiRes<BranchDto.BranchResponse>> createBranch(
			@Parameter(description = "Branch creation payload", required = true)
			@Valid @RequestBody BranchDto.BranchRequest request) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		BranchDto.BranchResponse created = branchService.createBranch(request, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(created));
	}

	@Operation(summary = "Update a branch", description = "Update an existing branch's details")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Branch updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid update data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Branch not found")
	})
	@PutMapping("/{branchId}")
	public ResponseEntity<ApiRes<BranchDto.BranchResponse>> updateBranch(
			@Parameter(description = "UUID of the branch to update", required = true)
			@PathVariable UUID branchId,
			@Parameter(description = "Updated branch details", required = true)
			@Valid @RequestBody BranchDto.BranchRequest request) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner() || UserContext.isRoleManager())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		BranchDto.BranchResponse updated = branchService.updateBranch(branchId, request, currentUserId, userRoles);
		return ResponseEntity.ok(ApiRes.success(updated));
	}

	@Operation(summary = "Delete a branch", description = "Remove a branch and all its associated data")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Branch deleted successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Branch not found")
	})
	@DeleteMapping("/{branchId}")
	public ResponseEntity<ApiRes<Void>> deleteBranch(
			@Parameter(description = "UUID of the branch to delete", required = true)
			@PathVariable UUID branchId) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		branchService.deleteBranch(branchId, currentUserId, userRoles);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Get all zones for a branch", description = "Retrieve list of zones for a specified branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Zones retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Branch not found")
	})
	@GetMapping("/{branchId}/zones")
	public ResponseEntity<ApiRes<List<ZoneDto.ZoneResponse>>> getZonesByBranch(
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId) {

		List<ZoneDto.ZoneResponse> zones = zoneService.getZonesByBranchId(branchId);
		return ResponseEntity.ok(ApiRes.success(zones));
	}

	/**
	 * Simple SVG validation example.
	 * You should use a library or custom sanitizer to remove scripts and unsafe elements.
	 */
	private boolean isValidSvg(String svgContent) {
		// Basic check: SVG must start with <svg and end with </svg>
		if (!svgContent.trim().startsWith("<svg") || !svgContent.trim().endsWith("</svg>")) {
			return false;
		}
		// Additional checks can be added here, e.g., disallow <script> tags
		return !svgContent.toLowerCase().contains("<script");
	}
}