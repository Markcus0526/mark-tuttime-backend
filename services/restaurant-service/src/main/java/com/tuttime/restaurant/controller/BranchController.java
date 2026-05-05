package com.tuttime.restaurant.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.restaurant.dto.BranchDto;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/restaurant-branches")
@Tag(name = "2.Branches of Restaurant", description = "Operations related to branches, schedules & media")
public class BranchController {

	private final BranchService branchService;

	// --- Branch CRUD ---

	@Operation(summary = "Get/Search all branches", description = "Retrieve paginated branches with filters for keyword, owner, and ratings.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Restaurants retrieved successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request parameters")
	})
	@GetMapping
	public ResponseEntity<ApiRes<Page<BranchDto.BranchResponse>>> getAllBranches(
			@Parameter(description = "Search by name or address")
			@RequestParam(required = false) List<UUID> tagIds,
			@RequestParam(required = false) String query,
			@RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice,
			@RequestParam(required = false) Double minRating,
			@RequestParam(required = false) Double maxRating,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "rating") String sort,
			@RequestParam(defaultValue = "desc") String direction) {

		// Cap the page size for performance
		int pageSize = Math.min(size, 100);

		Sort sortOrder = direction.equalsIgnoreCase("asc") ? Sort.by(sort).ascending() : Sort.by(sort).descending();
		Pageable pageable = PageRequest.of(page, pageSize, sortOrder);

		Page<BranchDto.BranchResponse> branches = branchService.findAllBranches(null, tagIds, query,
				minPrice, maxPrice, minRating, maxRating, pageable);
		return ResponseEntity.ok(ApiRes.success(branches));
	}

	@Operation(summary = "Get a branch by ID", description = "Retrieve detailed information about a branch by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Branch retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Branch not found")
	})
	@GetMapping("/{branchId}")
	public ResponseEntity<ApiRes<BranchDto.BranchDetailResponse>> getBranchById(
			@Parameter(description = "UUID of the branch to retrieve", required = true)
			@PathVariable UUID branchId) {

		BranchDto.BranchDetailResponse branch = branchService.getBranchById(branchId);
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
			@RequestParam(required = false) List<UUID> tagIds,
			@RequestParam(required = false) String query,
			@RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice,
			@RequestParam(required = false) Double minRating,
			@RequestParam(required = false) Double maxRating,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "name") String sort,
			@RequestParam(defaultValue = "asc") String direction) {

		// Cap the page size for performance
		int pageSize = Math.min(size, 100);

		Sort sortOrder = direction.equalsIgnoreCase("asc") ? Sort.by(sort).ascending() : Sort.by(sort).descending();
		Pageable pageable = PageRequest.of(page, pageSize, sortOrder);

		Page<BranchDto.BranchResponse> branches = branchService.findAllBranches(restaurantId, tagIds, query,
				minPrice, maxPrice, minRating, maxRating, pageable);
		return ResponseEntity.ok(ApiRes.success(branches));
	}

	@Operation(summary = "Get all master tags", description = "Retrieve a list of all available tags for branches.")
	@GetMapping("/tags")
	public ResponseEntity<ApiRes<List<BranchDto.TagResponse>>> getAllTags() {
		List<BranchDto.TagResponse> tags = branchService.getAllTags();
		return ResponseEntity.ok(ApiRes.success(tags));
	}

	@Operation(summary = "Sync branch tags", description = "Add, remove, or create tags for a specific branch. If a tag name doesn't exist globally, it will be created.")
	@PutMapping("/{branchId}/tags")
	public ResponseEntity<ApiRes<Void>> syncBranchTags(
			@PathVariable UUID branchId,
			@Valid @RequestBody List<BranchDto.TagRequest> tags) {

		// 1. Existing security check in your controller
		validateAdminOrOwnerOrManager();

		// 2. Extract context for service-level checks
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		// 3. Delegate to service for the "find or create" logic
		branchService.syncBranchTags(branchId, tags, currentUserId, userRoles);

		return ResponseEntity.ok(ApiRes.success(null));
	}

	@PostMapping("/{branchId}/bookmark")
	public ResponseEntity<ApiRes<Boolean>> toggleBookmark(@PathVariable UUID branchId) {

		validateLogin();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());

		boolean status = branchService.toggleBookmark(branchId, currentUserId);
		return ResponseEntity.ok(ApiRes.success(status));
	}

	// --- Restaurant Search by near ---

	@Operation(summary = "Find branches near a location", description = "Retrieve branches within a radius of given coordinates, optionally filtered by tag IDs.")
	@GetMapping("/near")
	public ResponseEntity<ApiRes<Page<BranchDto.BranchResponse>>> findBranchesNear(
			@RequestParam double latitude,
			@RequestParam double longitude,
			@RequestParam(defaultValue = "1000") double radiusMeters,
			@RequestParam(required = false) List<UUID> tagIds,
			@RequestParam(required = false) String query,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "rating") String sort,
			@RequestParam(defaultValue = "desc") String direction) {

		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());

		int pageSize = Math.min(size, 100);
		Sort sortOrder = direction.equalsIgnoreCase("asc") ? Sort.by(sort).ascending() : Sort.by(sort).descending();
		Pageable pageable = PageRequest.of(page, pageSize, sortOrder);

		Page<BranchDto.BranchResponse> branches = branchService.searchMap(
				latitude, longitude, radiusMeters, tagIds, query, pageable, currentUserId);
		return ResponseEntity.ok(ApiRes.success(branches));
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