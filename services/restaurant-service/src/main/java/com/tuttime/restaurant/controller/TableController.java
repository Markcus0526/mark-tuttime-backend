package com.tuttime.restaurant.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.restaurant.dto.TableRequest;
import com.tuttime.restaurant.dto.TableResponse;
import com.tuttime.restaurant.entity.enums.TableStatus;
import com.tuttime.restaurant.service.BranchService;
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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/tables")
@Tag(name = "Tables", description = "Operations related to tables within zones of branches")
public class TableController {

	private final BranchService branchService;
	private final TableService tableService;

	@Operation(summary = "Get a table by ID", description = "Retrieve details of a specific table")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Table retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Table not found")
	})
	@GetMapping("/{tableId}")
	public ResponseEntity<ApiRes<TableResponse>> getTableById(
			@Parameter(description = "UUID of the table", required = true)
			@PathVariable UUID tableId) {

		TableResponse table = tableService.getTableById(tableId);
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
	public ResponseEntity<ApiRes<TableResponse>> createTable(
			@Parameter(description = "Table creation payload", required = true)
			@Valid @RequestBody TableRequest request) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner() || UserContext.isRoleManager())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		TableResponse created = tableService.createTable(request, currentUserId, userRoles);
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
	public ResponseEntity<ApiRes<TableResponse>> updateTable(
			@Parameter(description = "UUID of the table", required = true)
			@PathVariable UUID tableId,
			@Parameter(description = "Updated table details", required = true)
			@Valid @RequestBody TableRequest request) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner() || UserContext.isRoleManager())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		// Service should confirm the table belongs to the zone, branch, restaurant
		TableResponse updated = tableService.updateTable(tableId, request, currentUserId, userRoles);
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

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner() || UserContext.isRoleManager())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		// Service should verify hierarchical ownership before deletion
		tableService.deleteTable(tableId, currentUserId, userRoles);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Get reservation status of a table")
	@GetMapping("/{tableId}/status")
	public ResponseEntity<ApiRes<TableStatus>> getTableStatus(
			@PathVariable UUID tableId) {

		TableStatus status = tableService.getTableCurrentStatus(tableId);
		return ResponseEntity.ok(ApiRes.success(status));
	}
}