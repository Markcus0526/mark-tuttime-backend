package com.tuttime.menu.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.menu.dto.DiscountDto;
import com.tuttime.menu.service.DiscountService;
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
@RequestMapping("/api/v1/menu-discounts")
@RequiredArgsConstructor
@Tag(name = "4.Discounts", description = "Coordinates price cuts and active campaigns")
public class DiscountController {

	private final DiscountService discountService;

	@PostMapping
	@Operation(summary = "Launch a discount rule", description = "Establishes a promotion on a designated category or targeted item.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Discount created",
					content = @Content(schema = @Schema(implementation = DiscountDto.DiscountResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request payload")
	})
	public ResponseEntity<ApiRes<DiscountDto.DiscountResponse>> createDiscount(@Valid @RequestBody DiscountDto.DiscountRequest request) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		DiscountDto.DiscountResponse data = discountService.createDiscount(request, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(data));
	}

	@GetMapping("/{discountId}")
	@Operation(summary = "Query exact discount parameters")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Success",
					content = @Content(schema = @Schema(implementation = DiscountDto.DiscountResponse.class))),
			@ApiResponse(responseCode = "404", description = "Discount not found")
	})
	public ResponseEntity<ApiRes<DiscountDto.DiscountResponse>> getDiscountById(@PathVariable UUID discountId) {

		DiscountDto.DiscountResponse data = discountService.getDiscountById(discountId);
		return ResponseEntity.ok(ApiRes.success(data));
	}

	@GetMapping("/active")
	@Operation(summary = "Fetch active active-period discounts", description = "Traces down running discounts attached to targeted objects.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "List of active discounts retrieved",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = DiscountDto.DiscountResponse.class))))
	})
	public ResponseEntity<ApiRes<List<DiscountDto.DiscountResponse>>> getActiveDiscounts(
			@Parameter(description = "UUID of the Category or Item") @RequestParam("referenceId") UUID referenceId) {

		List<DiscountDto.DiscountResponse> data = discountService.getActiveDiscountsForReference(referenceId);
		return ResponseEntity.ok(ApiRes.success(data));
	}

	@DeleteMapping("/{discountId}")
	@Operation(summary = "Cancel a promotion")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Discount permanently dropped"),
			@ApiResponse(responseCode = "404", description = "Discount not found")
	})
	public ResponseEntity<ApiRes<Void>> deleteDiscount(@PathVariable UUID discountId) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		discountService.deleteDiscount(discountId, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
	}
}
