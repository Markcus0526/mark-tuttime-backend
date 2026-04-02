package com.tuttime.pos.controller;

import com.tuttime.common.dto.ApiRes;
import com.tuttime.pos.dto.*;
import com.tuttime.pos.entity.PosSystemConfig;
import com.tuttime.pos.service.PosSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pos")
@RequiredArgsConstructor
@Tag(name = "POS Management", description = "Operations for restaurant terminal configuration and synchronization audit")
public class PosController {

	private final PosSyncService posSyncService;

	@PostMapping("/sync/booking")
	@Operation(summary = "Manual Booking Sync", description = "Triggers a manual push of a booking to the restaurant terminal.")
	@ApiResponse(responseCode = "202", description = "Synchronization task accepted")
	public ResponseEntity<ApiRes<Void>> syncBooking(@Valid @RequestBody BookingSyncRequest request) {
		posSyncService.syncBooking(request);
		return ResponseEntity.accepted().body(ApiRes.success(null));
	}

	@PostMapping("/sync/order")
	@Operation(summary = "Manual Order Sync", description = "Forces an order synchronization. Note: This usually happens automatically via Kafka.")
	@ApiResponse(responseCode = "202", description = "Order synchronization task accepted")
	public ResponseEntity<ApiRes<Void>> syncOrder(@Valid @RequestBody OrderSyncRequest request) {
		posSyncService.syncOrder(request);
		return ResponseEntity.accepted().body(ApiRes.success(null));
	}

	@PostMapping("/sync/menu")
	@Operation(summary = "Sync Menu Catalog", description = "Pulls latest items and prices from the physical POS (iiko/R-Keeper) to the cloud.")
	public ResponseEntity<ApiRes<Void>> syncMenu(@Valid @RequestBody MenuSyncRequest request) {
		posSyncService.syncMenu(request);
		return ResponseEntity.accepted().body(ApiRes.success(null));
	}

	@GetMapping("/logs/entity/{entityId}")
	@Operation(summary = "Retrieve Entity Logs", description = "Fetches the full history of POS sync attempts for a specific Order or Booking UUID.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Logs found",
					content = @Content(schema = @Schema(implementation = PosSyncLogResponse.class))),
			@ApiResponse(responseCode = "404", description = "No logs found for this entity",
					content = @Content(schema = @Schema(implementation = ApiRes.class)))
	})
	public ResponseEntity<ApiRes<PosSyncLogResponse>> getLogsByEntityId(
			@Parameter(description = "UUID of the target entity", example = "550e8400-e29b-41d4-a716-446655440000")
			@PathVariable UUID entityId) {
		PosSyncLogResponse response = posSyncService.getSyncLogs(entityId);
		return ResponseEntity.ok(ApiRes.success(response));
	}

	@PostMapping("/credentials")
	@Operation(summary = "Configure POS Credentials", description = "Registers or updates API endpoints and secrets for a restaurant's POS system.")
	@ApiResponse(responseCode = "201", description = "Configuration saved")
	public ResponseEntity<ApiRes<PosSystemConfig>> saveCredentials(@Valid @RequestBody PosCredentialsDto credentialsDto) {
		PosSystemConfig saved = posSyncService.saveConfig(credentialsDto);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiRes.success(saved));
	}

	@PostMapping("/force-sync/{restaurantId}")
	@Operation(summary = "Force Resync Restaurant", description = "Bulk retries all failed sync tasks for a specific restaurant ID.")
	public ResponseEntity<ApiRes<String>> forceSync(
			@Parameter(description = "Restaurant UUID") @PathVariable UUID restaurantId) {
		posSyncService.forceSyncByRestaurantId(restaurantId);
		return ResponseEntity.accepted().body(ApiRes.success(
				"Bulk sync triggered for restaurant: " + restaurantId,
				"Operation started."
		));
	}
}
