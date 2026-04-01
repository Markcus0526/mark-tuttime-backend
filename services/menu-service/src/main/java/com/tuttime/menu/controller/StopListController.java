package com.tuttime.menu.controller;

import com.tuttime.common.dto.ApiRes;
import com.tuttime.menu.dto.StopListDto;
import com.tuttime.menu.service.StopListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/menus/stoplists")
@RequiredArgsConstructor
@Tag(name = "Stop Lists", description = "Controls instantaneous out-of-stock actions per branch")
public class StopListController {

	private final StopListService stopListService;

	@PostMapping
	@Operation(summary = "Place item on stop list", description = "Marks a specific menu item as unavailable for a branch due to raw material deficits.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Item added to stop list",
					content = @Content(schema = @Schema(implementation = StopListDto.StopListResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request or item already stopped")
	})
	public ResponseEntity<ApiRes<StopListDto.StopListResponse>> addToStopList(@Valid @RequestBody StopListDto.StopListRequest request) {
		StopListDto.StopListResponse data = stopListService.addToStopList(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(data));
	}

	@GetMapping("/branch/{branchId}")
	@Operation(summary = "Gather full branch stop list", description = "Aids localized portals in reviewing stopped operations.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "List of stopped items retrieved",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = StopListDto.StopListResponse.class))))
	})
	public ResponseEntity<ApiRes<List<StopListDto.StopListResponse>>> getStopListByBranch(
			@Parameter(description = "UUID of the branch") @PathVariable UUID branchId) {
		List<StopListDto.StopListResponse> data = stopListService.getStopListByBranch(branchId);
		return ResponseEntity.ok(ApiRes.success(data));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Restore item stock status", description = "Removes the item from the stop list, making it available for orders again.")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Item restored to active status"),
			@ApiResponse(responseCode = "404", description = "Stop list entry not found")
	})
	public ResponseEntity<Void> removeFromStopList(@PathVariable UUID id) {
		stopListService.removeFromStopList(id);
		return ResponseEntity.noContent().build();
	}
}
