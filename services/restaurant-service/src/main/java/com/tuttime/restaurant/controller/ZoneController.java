package com.tuttime.restaurant.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.restaurant.dto.TableResponse;
import com.tuttime.restaurant.dto.ZoneRequest;
import com.tuttime.restaurant.dto.ZoneResponse;
import com.tuttime.restaurant.service.BranchService;
import com.tuttime.restaurant.service.MediaService;
import com.tuttime.restaurant.service.TableService;
import com.tuttime.restaurant.service.ZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/zones")
@Tag(name = "Zones", description = "Operations related to zones within branches of restaurants")
public class ZoneController {

	private final BranchService branchService;
	private final ZoneService zoneService;
	private final MediaService mediaService;
	private final TableService tableService;
	// --- Zone CRUD ---

	@Operation(summary = "Get zone by ID", description = "Retrieve detailed information about a zone")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Zone retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Zone not found")
	})
	@GetMapping("/{zoneId}")
	public ResponseEntity<ApiRes<ZoneResponse>> getZoneById(
			@Parameter(description = "UUID of the zone", required = true)
			@PathVariable UUID zoneId) {

		ZoneResponse zone = zoneService.getZoneById(zoneId);
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
	public ResponseEntity<ApiRes<ZoneResponse>> createZone(
			@Parameter(description = "Zone creation payload", required = true)
			@Valid @RequestBody ZoneRequest request) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner() || UserContext.isRoleManager())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		ZoneResponse created = zoneService.createZone(request, currentUserId, userRoles);
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
	public ResponseEntity<ApiRes<ZoneResponse>> updateZone(
			@Parameter(description = "UUID of the zone", required = true)
			@PathVariable UUID zoneId,
			@Parameter(description = "Updated zone details", required = true)
			@Valid @RequestBody ZoneRequest request) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner() || UserContext.isRoleManager())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		ZoneResponse updated = zoneService.updateZone(zoneId, request, currentUserId, userRoles);
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

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner() || UserContext.isRoleManager())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		zoneService.deleteZone(zoneId, currentUserId, userRoles);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Get all tables in a zone", description = "Retrieve list of tables within a specific zone")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Tables retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Zone not found")
	})
	@GetMapping("/{zoneId}/tables")
	public ResponseEntity<ApiRes<List<TableResponse>>> getTablesInZone(
			@Parameter(description = "UUID of the zone", required = true)
			@PathVariable UUID zoneId) {

		List<TableResponse> tables = tableService.getTablesByZoneId(zoneId);
		return ResponseEntity.ok(ApiRes.success(tables));
	}

	// Reservation

	@Operation(summary = "Get available tables in a zone for a time range")
	@GetMapping("/{zoneId}/available")
	public ResponseEntity<ApiRes<List<TableResponse>>> getAvailableTables(
			@PathVariable UUID zoneId,
			@RequestParam
			@Parameter(description = "ISO 8601 format (e.g. 2026-04-07T14:00:00+02:00)", required = true)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
			OffsetDateTime from,
			@RequestParam
			@Parameter(description = "ISO 8601 format (e.g. 2026-04-07T16:00:00+02:00)", required = true)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
			OffsetDateTime to) {

		List<TableResponse> availableTables = tableService.findAvailableTables(zoneId, from, to);
		return ResponseEntity.ok(ApiRes.success(availableTables));
	}
}
