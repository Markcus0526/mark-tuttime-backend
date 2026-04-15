package com.tuttime.restaurant.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.restaurant.dto.TableDto;
import com.tuttime.restaurant.entity.enums.TableStatus;
import com.tuttime.restaurant.service.TableService;
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
@RequestMapping("/api/v1/restaurant-tables")
@Tag(name = "4.Restaurant Tables", description = "Operations related to tables within zones of branches")
public class TableController {

	private final TableService tableService;

	@Operation(summary = "Get a table by ID", description = "Retrieve details of a specific table")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Table retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Table not found")
	})
	@GetMapping("/{tableId}")
	public ResponseEntity<ApiRes<TableDto.TableResponse>> getTableById(
			@Parameter(description = "UUID of the table", required = true)
			@PathVariable UUID tableId) {

		TableDto.TableResponse table = tableService.getTableById(tableId);
		return ResponseEntity.ok(ApiRes.success(table));
	}

	@Operation(summary = "Add a new table to a zone", description = "Create a new table within a specified zone")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Table created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid table data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Zone not found")
	})
	@PostMapping
	public ResponseEntity<ApiRes<TableDto.TableResponse>> createTable(
			@Parameter(description = "Table creation payload", required = true)
			@Valid @RequestBody TableDto.TableRequest request) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		TableDto.TableResponse created = tableService.createTable(request, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(created));
	}

	@Operation(summary = "Update a table", description = "Update details of an existing table")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Table updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid table data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Table not found")
	})
	@PutMapping("/{tableId}")
	public ResponseEntity<ApiRes<TableDto.TableResponse>> updateTable(
			@Parameter(description = "UUID of the table", required = true)
			@PathVariable UUID tableId,
			@Parameter(description = "Updated table details", required = true)
			@Valid @RequestBody TableDto.TableRequest request) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		// Service should confirm the table belongs to the zone, branch, restaurant
		TableDto.TableResponse updated = tableService.updateTable(tableId, request, currentUserId, userRoles);
		return ResponseEntity.ok(ApiRes.success(updated));
	}

	@Operation(summary = "Delete a table", description = "Remove a table from a zone")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Table deleted successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Table not found")
	})
	@DeleteMapping("/{tableId}")
	public ResponseEntity<ApiRes<Void>> deleteTable(
			@Parameter(description = "UUID of the table", required = true)
			@PathVariable UUID tableId) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		// Service should verify hierarchical ownership before deletion
		tableService.deleteTable(tableId, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
	}

	@Operation(summary = "Get reservation status of a table")
	@GetMapping("/{tableId}/status")
	public ResponseEntity<ApiRes<TableStatus>> getTableStatus(
			@PathVariable UUID tableId) {

		TableStatus status = tableService.getTableCurrentStatus(tableId);
		return ResponseEntity.ok(ApiRes.success(status));
	}

	@Operation(summary = "Get all tables in a zone", description = "Retrieve list of tables within a specific zone")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Tables retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Zone not found")
	})
	@GetMapping("/zones/{zoneId}")
	public ResponseEntity<ApiRes<List<TableDto.TableResponse>>> getTablesInZone(
			@Parameter(description = "UUID of the zone", required = true)
			@PathVariable UUID zoneId) {

		List<TableDto.TableResponse> tables = tableService.getTablesByZoneId(zoneId);
		return ResponseEntity.ok(ApiRes.success(tables));
	}

	@Operation(summary = "Get available tables in a zone for a time range")
	@GetMapping("/zones/{zoneId}/available")
	public ResponseEntity<ApiRes<List<TableDto.TableResponse>>> getAvailableTables(
			@PathVariable UUID zoneId,
			@RequestParam
			@Parameter(description = "ISO 8601 format (e.g. 2026-04-07T14:00:00+02:00)", required = true)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
			OffsetDateTime from,
			@RequestParam
			@Parameter(description = "ISO 8601 format (e.g. 2026-04-07T16:00:00+02:00)", required = true)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
			OffsetDateTime to) {

		List<TableDto.TableResponse> availableTables = tableService.findAvailableTables(zoneId, from, to);
		return ResponseEntity.ok(ApiRes.success(availableTables));
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