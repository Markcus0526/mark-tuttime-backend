package com.tuttime.restaurant.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.restaurant.dto.ZoneDto;
import com.tuttime.restaurant.dto.ZoneLayoutDto;
import com.tuttime.restaurant.service.ZoneLayoutService;
import com.tuttime.restaurant.service.ZoneService;
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
@RequestMapping("/api/v1/restaurant-zones")
@Tag(name = "3.Restaurant Zones", description = "Operations related to zones within branches of restaurants")
public class ZoneController {

	private final ZoneService zoneService;
	private final ZoneLayoutService zoneLayoutService;

	// --- Zone CRUD ---

	@Operation(summary = "Get zone by ID", description = "Retrieve detailed information about a zone")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Zone retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Zone not found")
	})
	@GetMapping("/{zoneId}")
	public ResponseEntity<ApiRes<ZoneDto.ZoneResponse>> getZoneById(
			@Parameter(description = "UUID of the zone", required = true)
			@PathVariable UUID zoneId) {

		ZoneDto.ZoneResponse zone = zoneService.getZoneById(zoneId);
		return ResponseEntity.ok(ApiRes.success(zone));
	}

	@Operation(summary = "Create a new zone", description = "Add a new zone to a branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Zone created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Branch not found")
	})
	@PostMapping
	public ResponseEntity<ApiRes<ZoneDto.ZoneResponse>> createZone(
			@Parameter(description = "Zone creation payload", required = true)
			@Valid @RequestBody ZoneDto.ZoneRequest request) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		ZoneDto.ZoneResponse created = zoneService.createZone(request, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(created));
	}

	@Operation(summary = "Update a zone", description = "Update details of an existing zone")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Zone updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid update data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Zone not found")
	})
	@PutMapping("/{zoneId}")
	public ResponseEntity<ApiRes<ZoneDto.ZoneResponse>> updateZone(
			@Parameter(description = "UUID of the zone", required = true)
			@PathVariable UUID zoneId,
			@Parameter(description = "Updated zone details", required = true)
			@Valid @RequestBody ZoneDto.ZoneRequest request) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		ZoneDto.ZoneResponse updated = zoneService.updateZone(zoneId, request, currentUserId, userRoles);
		return ResponseEntity.ok(ApiRes.success(updated));
	}

	@Operation(summary = "Delete a zone", description = "Remove a zone from a branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Zone deleted successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Zone not found")
	})
	@DeleteMapping("/{zoneId}")
	public ResponseEntity<ApiRes<Void>> deleteZone(
			@Parameter(description = "UUID of the zone", required = true)
			@PathVariable UUID zoneId) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		zoneService.deleteZone(zoneId, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
	}

	@Operation(summary = "Get all zones for a branch", description = "Retrieve list of zones for a specified branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Zones retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Branch not found")
	})
	@GetMapping("/branches/{branchId}")
	public ResponseEntity<ApiRes<List<ZoneDto.ZoneResponse>>> getZonesByBranch(
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId) {

		List<ZoneDto.ZoneResponse> zones = zoneService.getZonesByBranchId(branchId);
		return ResponseEntity.ok(ApiRes.success(zones));
	}

	/**
	 * GET /api/v1/zones/{zoneId}/layout
	 * Retrieves the visual layout, fixtures, and table positions for the admin panel.
	 */
	@GetMapping("/{zoneId}/layout")
	public ResponseEntity<ApiRes<ZoneLayoutDto.LayoutDto>> getLayout(
			@PathVariable UUID zoneId) {

		ZoneLayoutDto.LayoutDto layout = zoneLayoutService.getZoneLayout(zoneId);
		return ResponseEntity.ok(ApiRes.success(layout));
	}

	/**
	 * PUT /api/v1/zones/{zoneId}/layout
	 * Synchronizes the layout from the admin panel (Inserts new, Updates moved, Soft-Deletes removed).
	 */
	@PutMapping("/{zoneId}/layout")
	public ResponseEntity<ApiRes<Void>> updateLayout(
			@PathVariable UUID zoneId,
			@RequestBody ZoneLayoutDto.LayoutDto layoutDto) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		zoneLayoutService.syncLayout(zoneId, layoutDto, currentUserId, userRoles);
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
