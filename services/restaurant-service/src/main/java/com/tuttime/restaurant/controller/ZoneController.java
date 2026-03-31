package com.tuttime.restaurant.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.restaurant.dto.ZoneRequest;
import com.tuttime.restaurant.dto.ZoneResponse;
import com.tuttime.restaurant.enums.EntityType;
import com.tuttime.restaurant.service.MediaService;
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
@RequestMapping("/api/v1/restaurants/{restaurantId}/branches/{branchId}/zones")
@Tag(name = "Zones", description = "Operations related to zones within branches of restaurants")
public class ZoneController {

	private final ZoneService zoneService;
	private final MediaService mediaService;

	// --- Zone CRUD ---

	@Operation(summary = "Get all zones for a branch", description = "Retrieve list of zones for a specified branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Zones retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Branch not found")
	})
	@GetMapping
	public ResponseEntity<ApiRes<List<ZoneResponse>>> getZonesByBranch(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId) {
		List<ZoneResponse> zones = zoneService.getZonesByBranchId(branchId);
		return ResponseEntity.ok(ApiRes.success(zones));
	}

	@Operation(summary = "Get zone by ID", description = "Retrieve detailed information about a zone")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Zone retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Zone not found")
	})
	@GetMapping("/{zoneId}")
	public ResponseEntity<ApiRes<ZoneResponse>> getZoneById(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId,
			@Parameter(description = "UUID of the zone", required = true)
			@PathVariable UUID zoneId) {
		ZoneResponse zone = zoneService.getZoneById(branchId, zoneId);
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
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId,
			@Parameter(description = "Zone creation payload", required = true)
			@Valid @RequestBody ZoneRequest request) {
		if (!UserContext.isRoleAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		ZoneResponse created = zoneService.createZone(branchId, request);
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
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId,
			@Parameter(description = "UUID of the zone", required = true)
			@PathVariable UUID zoneId,
			@Parameter(description = "Updated zone details", required = true)
			@Valid @RequestBody ZoneRequest request) {
		if (!UserContext.isRoleAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		ZoneResponse updated = zoneService.updateZone(branchId, zoneId, request);
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
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId,
			@Parameter(description = "UUID of the zone", required = true)
			@PathVariable UUID zoneId) {
		if (!UserContext.isRoleAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		zoneService.deleteZone(branchId, zoneId);
		return ResponseEntity.noContent().build();
	}

	// --- Zone Map (Media SVG) ---

	@Operation(summary = "Get zone map SVG", description = "Retrieve the inline SVG map for a zone")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Zone map SVG retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Zone or map not found")
	})
	@GetMapping("/{zoneId}/map")
	public ResponseEntity<ApiRes<String>> getZoneMapSvg(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the zone", required = true)
			@PathVariable UUID zoneId) {
		String svgContent = mediaService.getPrimarySvgMap(EntityType.ZONE, zoneId);
		return ResponseEntity.ok(ApiRes.success(svgContent));
	}

	@Operation(summary = "Set or update zone map SVG", description = "Upload or update the inline SVG map for a zone")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Zone map SVG set/updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid SVG content"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Zone not found")
	})
	@PostMapping("/{zoneId}/map")
	public ResponseEntity<ApiRes<Void>> setZoneMapSvg(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId,
			@Parameter(description = "UUID of the zone", required = true)
			@PathVariable UUID zoneId,
			@Parameter(description = "SVG XML content", required = true)
			@RequestBody String svgContent) {
		if (!UserContext.isRoleAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		mediaService.setOrUpdateSvgMap(EntityType.ZONE, zoneId, svgContent);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(null));
	}
}