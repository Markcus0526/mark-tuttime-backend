package com.tuttime.pos.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.pos.dto.PosConfigDto;
import com.tuttime.pos.dto.PosSyncDto;
import com.tuttime.pos.dto.PosSyncLogDto;
import com.tuttime.pos.entity.PosConfig;
import com.tuttime.pos.service.PosService;
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
@RequestMapping("/api/v1/poses")
@RequiredArgsConstructor
@Tag(name = "POS Management", description = "Operations for branch terminal configuration and synchronization audit")
public class PosController {

	private final PosService posService;

	@PostMapping("/config")
	@Operation(summary = "Set Branch POS Configuration")
	public ResponseEntity<ApiRes<PosConfig>> configureBranchPos(@Valid @RequestBody PosConfigDto.PosConfigRequest request) {

		validateAdminOrOwner(); // Security check

		PosConfig saved = posService.saveConfig(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiRes.success(saved));
	}

	@PostMapping("/sync/booking")
	@Operation(summary = "Manual Booking Sync", description = "Triggers a manual push of a booking to the branch terminal.")
	@ApiResponse(responseCode = "202", description = "Synchronization task accepted")
	public ResponseEntity<ApiRes<Void>> syncBooking(
			@Valid @RequestBody PosSyncDto.BookingSyncRequest request) {

		validateAdminOrOwnerOrManager();

		posService.syncBooking(request);
		return ResponseEntity.accepted().body(ApiRes.success(null));
	}

	@PostMapping("/sync/order")
	@Operation(summary = "Manual Order Sync", description = "Forces an order synchronization. Note: This usually happens automatically via Kafka.")
	@ApiResponse(responseCode = "202", description = "Order synchronization task accepted")
	public ResponseEntity<ApiRes<Void>> syncOrder(
			@Valid @RequestBody PosSyncDto.OrderSyncRequest request) {

		validateAdminOrOwnerOrManager();

		posService.syncOrder(request);
		return ResponseEntity.accepted().body(ApiRes.success(null));
	}

	@PostMapping("/sync/menu")
	@Operation(summary = "Sync Menu Catalog", description = "Pulls latest items and prices from the physical POS (iiko/R-Keeper) to the cloud.")
	public ResponseEntity<ApiRes<Void>> syncMenu(
			@Valid @RequestBody PosSyncDto.MenuSyncRequest request) {

		validateAdminOrOwner();

		posService.syncMenu(request);
		return ResponseEntity.accepted().body(ApiRes.success(null));
	}

	@GetMapping("/logs/entity/{entityId}")
	@Operation(summary = "Retrieve Entity Logs", description = "Fetches the full history of POS sync attempts for a specific Order or Booking UUID.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Logs found",
					content = @Content(schema = @Schema(implementation = PosSyncLogDto.PosSyncLogResponse.class))),
			@ApiResponse(responseCode = "404", description = "No logs found for this entity",
					content = @Content(schema = @Schema(implementation = ApiRes.class)))
	})
	public ResponseEntity<ApiRes<PosSyncLogDto.PosSyncLogResponse>> getLogsByEntityId(
			@Parameter(description = "UUID of the target entity", example = "550e8400-e29b-41d4-a716-446655440000")
			@PathVariable UUID entityId) {

		validateAdminOrOwnerOrManager();

		PosSyncLogDto.PosSyncLogResponse response = posService.getSyncLogs(entityId);
		return ResponseEntity.ok(ApiRes.success(response));
	}

	@PostMapping("/sync/{branchId}")
	@Operation(summary = "Force Resync Branch", description = "Bulk retries all failed sync tasks for a specific branch ID.")
	public ResponseEntity<ApiRes<String>> forceSync(
			@Parameter(description = "Branch UUID") @PathVariable UUID branchId) {

		validateAdminOrOwnerOrManager();

		posService.forceSyncByBranchId(branchId);
		return ResponseEntity.accepted().body(ApiRes.success(
				"Bulk sync triggered for branch: " + branchId,
				"Operation started."
		));
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
