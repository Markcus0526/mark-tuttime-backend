package com.tuttime.restaurant.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.restaurant.dto.BranchDto;
import com.tuttime.restaurant.dto.RestaurantDto;
import com.tuttime.restaurant.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/restaurant-branches")
@Tag(name = "2.Restaurant Branches", description = "Operations related to branches, schedules & media")
public class BranchController {

	private final BranchService branchService;

	// --- Branch CRUD ---

	@Operation(summary = "Get a branch by ID", description = "Retrieve detailed information about a branch by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Branch retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Branch not found")
	})
	@GetMapping("/{branchId}")
	public ResponseEntity<ApiRes<BranchDto.BranchDetailedResponse>> getBranchById(
			@Parameter(description = "UUID of the branch to retrieve", required = true)
			@PathVariable UUID branchId) {

		BranchDto.BranchDetailedResponse branch = branchService.getBranchById(branchId);
		return ResponseEntity.ok(ApiRes.success(branch));
	}

	@Operation(summary = "Create a new branch", description = "Add a new branch to a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Branch created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input data"),
			@ApiResponse(responseCode = "403", description = "Access denied")
	})
	@PostMapping
	public ResponseEntity<ApiRes<BranchDto.BranchResponse>> createBranch(
			@Parameter(description = "Branch creation payload", required = true)
			@Valid @RequestBody BranchDto.BranchRequest request) {

		validateAdminOrOwner();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		BranchDto.BranchResponse created = branchService.createBranch(request, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(created));
	}

	@Operation(summary = "Update a branch", description = "Update an existing branch's details")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Branch updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid update data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Branch not found")
	})
	@PutMapping("/{branchId}")
	public ResponseEntity<ApiRes<BranchDto.BranchResponse>> updateBranch(
			@Parameter(description = "UUID of the branch to update", required = true)
			@PathVariable UUID branchId,
			@Parameter(description = "Updated branch details", required = true)
			@Valid @RequestBody BranchDto.BranchRequest request) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		BranchDto.BranchResponse updated = branchService.updateBranch(branchId, request, currentUserId, userRoles);
		return ResponseEntity.ok(ApiRes.success(updated));
	}

	@Operation(summary = "Delete a branch", description = "Remove a branch and all its associated data")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Branch deleted successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Branch not found")
	})
	@DeleteMapping("/{branchId}")
	public ResponseEntity<ApiRes<Void>> deleteBranch(
			@Parameter(description = "UUID of the branch to delete", required = true)
			@PathVariable UUID branchId) {

		validateAdminOrOwner();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		branchService.deleteBranch(branchId, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
	}

	@Operation(summary = "Get all branches", description = "Retrieve paginated list of branches with optional filters")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "List of branches retrieved successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request parameters")
	})
	@GetMapping("/restaurants/{restaurantId}")
	public ResponseEntity<ApiRes<Page<BranchDto.BranchResponse>>> getAllBranches(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "Search by name or address")
			@RequestParam(required = false) String query,

			@RequestParam(required = false) Double minRating,
			@RequestParam(required = false) Double maxRating,

			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "createdAt") String sort,
			@RequestParam(defaultValue = "desc") String direction) {

		// Cap the page size for performance
		int pageSize = Math.min(size, 100);

		Sort sortOrder = direction.equalsIgnoreCase("asc") ? Sort.by(sort).ascending() : Sort.by(sort).descending();
		Pageable pageable = PageRequest.of(page, pageSize, sortOrder);

		Page<BranchDto.BranchResponse> branches = branchService.findAllBranches(restaurantId, query, minRating, maxRating, pageable);
		return ResponseEntity.ok(ApiRes.success(branches));
	}

	/**
	 * Simple SVG validation example.
	 * You should use a library or custom sanitizer to remove scripts and unsafe elements.
	 */
	private boolean isValidSvg(String svgContent) {
		// Basic check: SVG must start with <svg and end with </svg>
		if (!svgContent.trim().startsWith("<svg") || !svgContent.trim().endsWith("</svg>")) {
			return false;
		}
		// Additional checks can be added here, e.g., disallow <script> tags
		return !svgContent.toLowerCase().contains("<script");
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