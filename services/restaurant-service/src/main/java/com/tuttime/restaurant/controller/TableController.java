package com.tuttime.restaurant.controller;

import com.tuttime.common.dto.ApiRes;
import com.tuttime.restaurant.dto.TableRequest;
import com.tuttime.restaurant.dto.TableResponse;
import com.tuttime.restaurant.enums.TableStatus;
import com.tuttime.restaurant.service.TableService;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/restaurants/{restaurantId}/branches/{branchId}/zones/{zoneId}/tables")
@Tag(name = "Tables", description = "Operations related to tables within zones of branches")
public class TableController {

	private final TableService tableService;

	@Operation(summary = "Get all tables in a zone", description = "Retrieve list of tables within a specific zone")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Tables retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Zone not found")
	})
	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'MANAGER', 'HOSTESS', 'USER', 'GUEST')")
	public ResponseEntity<ApiRes<List<TableResponse>>> getTablesInZone(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId,
			@Parameter(description = "UUID of the zone", required = true)
			@PathVariable UUID zoneId) {
		// Service should verify zone belongs to branch and restaurant for data integrity
		List<TableResponse> tables = tableService.getTablesByZoneId(zoneId);
		return ResponseEntity.ok(ApiRes.success(tables));
	}

	@Operation(summary = "Get a table by ID", description = "Retrieve details of a specific table")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Table retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Table not found")
	})
	@GetMapping("/{tableId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'MANAGER', 'HOSTESS', 'USER', 'GUEST')")
	public ResponseEntity<ApiRes<TableResponse>> getTableById(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId,
			@Parameter(description = "UUID of the zone", required = true)
			@PathVariable UUID zoneId,
			@Parameter(description = "UUID of the table", required = true)
			@PathVariable UUID tableId) {
		// Service should validate table belongs to zone, branch, and restaurant
		TableResponse table = tableService.getTableById(tableId, zoneId);
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
	@PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(#restaurantId, @securityUtil.getCurrentUserId()) or hasRole('MANAGER')")
	public ResponseEntity<ApiRes<TableResponse>> createTable(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId,
			@Parameter(description = "UUID of the zone", required = true)
			@PathVariable UUID zoneId,
			@Parameter(description = "Table creation payload", required = true)
			@Valid @RequestBody TableRequest request) {
		// Service should validate zone -> branch -> restaurant hierarchy
		TableResponse created = tableService.createTable(zoneId, request);
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
	@PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(#restaurantId, @securityUtil.getCurrentUserId()) or hasRole('MANAGER')")
	public ResponseEntity<ApiRes<TableResponse>> updateTable(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId,
			@Parameter(description = "UUID of the zone", required = true)
			@PathVariable UUID zoneId,
			@Parameter(description = "UUID of the table", required = true)
			@PathVariable UUID tableId,
			@Parameter(description = "Updated table details", required = true)
			@Valid @RequestBody TableRequest request) {
		// Service should confirm the table belongs to the zone, branch, restaurant
		TableResponse updated = tableService.updateTable(tableId, zoneId, request);
		return ResponseEntity.ok(ApiRes.success(updated));
	}

	@Operation(summary = "Delete a table", description = "Remove a table from a zone")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Table deleted successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Table not found")
	})
	@DeleteMapping("/{tableId}")
	@PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(#restaurantId, @securityUtil.getCurrentUserId()) or hasRole('MANAGER')")
	public ResponseEntity<Void> deleteTable(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId,
			@Parameter(description = "UUID of the zone", required = true)
			@PathVariable UUID zoneId,
			@Parameter(description = "UUID of the table", required = true)
			@PathVariable UUID tableId) {
		// Service should verify hierarchical ownership before deletion
		tableService.deleteTable(tableId, zoneId);
		return ResponseEntity.noContent().build();
	}
	
	// Reservation
	
	@Operation(summary = "Get available tables in a zone for a time range")
	@GetMapping("/available")
	@PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'MANAGER', 'HOSTESS', 'USER')")
	public ResponseEntity<ApiRes<List<TableResponse>>> getAvailableTables(
			@PathVariable UUID restaurantId,
			@PathVariable UUID branchId,
			@PathVariable UUID zoneId,
			@RequestParam @Parameter(description = "Reservation start time in ISO 8601 format", required = true)
			Instant from,
			@RequestParam @Parameter(description = "Reservation end time in ISO 8601 format", required = true)
			Instant to) {

		List<TableResponse> availableTables = tableService.findAvailableTables(zoneId, from, to);
		return ResponseEntity.ok(ApiRes.success(availableTables));
	}

	@Operation(summary = "Get reservation status of a table")
	@GetMapping("/{tableId}/status")
	@PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'MANAGER', 'HOSTESS', 'USER')")
	public ResponseEntity<ApiRes<TableStatus>> getTableStatus(
			@PathVariable UUID restaurantId,
			@PathVariable UUID branchId,
			@PathVariable UUID zoneId,
			@PathVariable UUID tableId) {

		TableStatus status = tableService.getTableCurrentStatus(tableId);
		return ResponseEntity.ok(ApiRes.success(status));
	}

	@Operation(summary = "Confirm or link a reservation to a table")
	@PostMapping("/{tableId}/reservations/{reservationId}/confirm")
	@PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(#restaurantId, @securityUtil.getCurrentUserId()) or hasRole('MANAGER')")
	public ResponseEntity<ApiRes<Void>> confirmReservation(
			@PathVariable UUID restaurantId,
			@PathVariable UUID branchId,
			@PathVariable UUID zoneId,
			@PathVariable UUID tableId,
			@PathVariable UUID reservationId) {

		tableService.confirmReservation(tableId, reservationId);
		return ResponseEntity.ok(ApiRes.success(null));
	}
}