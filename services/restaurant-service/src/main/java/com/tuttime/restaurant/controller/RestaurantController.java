package com.tuttime.restaurant.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.restaurant.dto.*;
import com.tuttime.restaurant.entity.*;
import com.tuttime.restaurant.entity.enums.*;
import com.tuttime.restaurant.service.*;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/restaurants")
@Tag(name = "Restaurants", description = "Operations related to restaurants, branches, zones, tables, schedules & media")
public class RestaurantController {

	private final RestaurantService restaurantService;
	private final ScheduleService scheduleService;
	private final MediaService mediaService;
	private final BranchService branchService;
	private final RestaurantStaffService staffService;

	// --- Restaurant CRUD ---

	@Operation(summary = "Get/Search all restaurants", description = "Retrieve paginated restaurants with filters for keyword, owner, and ratings.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Restaurants retrieved successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request parameters")
	})
	@GetMapping
	public ResponseEntity<ApiRes<Page<RestaurantResponse>>> getAllRestaurants(
			@Parameter(description = "Search by name or address")
			@RequestParam(required = false) String query,

			@Parameter(description = "Filter by specific owner")
			@RequestParam(required = false) UUID ownerUserId,

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

		Page<RestaurantResponse> restaurants = restaurantService.findRestaurants(query, ownerUserId, minRating, maxRating, pageable);
		return ResponseEntity.ok(ApiRes.success(restaurants));
	}

	@Operation(summary = "Get a restaurant by ID", description = "Retrieve detailed information about a restaurant by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Restaurant retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@GetMapping("/{restaurantId}")
	public ResponseEntity<ApiRes<RestaurantResponse>> getRestaurantById(
			@Parameter(description = "UUID of the restaurant to retrieve", required = true)
			@PathVariable UUID restaurantId) {

		RestaurantResponse restaurant = restaurantService.getRestaurantById(restaurantId);
		return ResponseEntity.ok(ApiRes.success(restaurant));
	}

	@Operation(summary = "Create a new restaurant", description = "Add a new restaurant to the system")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Restaurant created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input data"),
			@ApiResponse(responseCode = "403", description = "Access denied")
	})
	@PostMapping
	public ResponseEntity<ApiRes<RestaurantResponse>> createRestaurant(
			@Parameter(description = "Restaurant creation payload", required = true)
			@Valid @RequestBody RestaurantRequest request) {

		if (!UserContext.isValidUUID() || !UserContext.isRoleAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		RestaurantResponse created = restaurantService.createRestaurant(request, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(created));
	}

	@Operation(summary = "Update a restaurant", description = "Update an existing restaurant's details")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Restaurant updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid update data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@PutMapping("/{restaurantId}")
	public ResponseEntity<ApiRes<RestaurantResponse>> updateRestaurant(
			@Parameter(description = "UUID of the restaurant to update", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "Updated restaurant details", required = true)
			@Valid @RequestBody RestaurantRequest request) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		RestaurantResponse updated = restaurantService.updateRestaurant(restaurantId, request, currentUserId, userRoles);
		return ResponseEntity.ok(ApiRes.success(updated));
	}

	@Operation(summary = "Delete a restaurant", description = "Remove a restaurant and all its associated data")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Restaurant deleted successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@DeleteMapping("/{restaurantId}")
	public ResponseEntity<ApiRes<Void>> deleteRestaurant(
			@Parameter(description = "UUID of the restaurant to delete", required = true)
			@PathVariable UUID restaurantId) {

		if (!UserContext.isValidUUID() || !UserContext.isRoleAdmin()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		restaurantService.deleteRestaurant(restaurantId, currentUserId, userRoles);
		return ResponseEntity.noContent().build();
	}

	// --- Restaurant Search by near ---

	@Operation(summary = "Find restaurants near a location", description = "Retrieve restaurants within a radius (in meters) of given latitude and longitude")
	@GetMapping("/near")
	public ResponseEntity<ApiRes<Page<RestaurantResponse>>> findRestaurantsNear(
			@RequestParam double latitude,
			@RequestParam double longitude,
			@RequestParam(defaultValue = "1000") double radiusMeters,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {

		int maxPageSize = 100;
		if (size > maxPageSize) size = maxPageSize;

		Page<RestaurantResponse> restaurants = restaurantService.findRestaurantsNearLocation(latitude, longitude, radiusMeters, page, size);
		return ResponseEntity.ok(ApiRes.success(restaurants));
	}

	// --- Restaurant Schedules ---

	@Operation(summary = "Get schedules for a restaurant", description = "Retrieve all schedule entries for a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Schedules retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@GetMapping("/{restaurantId}/schedules")
	public ResponseEntity<ApiRes<List<ScheduleResponse>>> getSchedules(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId) {

		List<ScheduleResponse> schedules = scheduleService.getSchedulesByRestaurantId(restaurantId);
		return ResponseEntity.ok(ApiRes.success(schedules));
	}

	@Operation(summary = "Add a schedule to a restaurant", description = "Create a new schedule entry for a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Schedule created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid schedule data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@PostMapping("/{restaurantId}/schedules")
	public ResponseEntity<ApiRes<ScheduleResponse>> addSchedule(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "Schedule details to create", required = true)
			@Valid @RequestBody ScheduleRequest request) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		ScheduleResponse created = scheduleService.createScheduleForRestaurant(restaurantId, request, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(created));
	}

	@Operation(summary = "Update a restaurant schedule", description = "Update a specific schedule entry for a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Schedule updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid schedule data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Schedule or restaurant not found")
	})
	@PutMapping("/{restaurantId}/schedules/{scheduleId}")
	public ResponseEntity<ApiRes<ScheduleResponse>> updateSchedule(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the schedule to update", required = true)
			@PathVariable UUID scheduleId,
			@Parameter(description = "Updated schedule details", required = true)
			@Valid @RequestBody ScheduleRequest request) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		ScheduleResponse updated = scheduleService.updateRestaurantSchedule(scheduleId, request, currentUserId, userRoles);
		return ResponseEntity.ok(ApiRes.success(updated));
	}

	@Operation(summary = "Delete a restaurant schedule", description = "Delete a specific schedule entry from a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Schedule deleted successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Schedule or restaurant not found")
	})
	@DeleteMapping("/{restaurantId}/schedules/{scheduleId}")
	public ResponseEntity<ApiRes<Void>> deleteSchedule(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the schedule to delete", required = true)
			@PathVariable UUID scheduleId) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		scheduleService.deleteRestaurantSchedule(scheduleId, currentUserId, userRoles);
		return ResponseEntity.noContent().build();
	}

	// --- Restaurant Media (Images, Videos, SVGs) ---

	@Operation(summary = "Get media for a restaurant", description = "Retrieve all media items associated with a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Media retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@GetMapping("/{restaurantId}/medias")
	public ResponseEntity<ApiRes<List<MediaResponse>>> getMediaForRestaurant(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId) {

		List<MediaResponse> medias = mediaService.getMediaByEntity(EntityType.RESTAURANT, restaurantId);
		return ResponseEntity.ok(ApiRes.success(medias));
	}

	@Operation(summary = "Add media to a restaurant", description = "Upload a new media item (image/video/svg) for a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Media added successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid media data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@PostMapping(value = "/{restaurantId}/medias", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiRes<MediaResponse>> addMediaToRestaurant(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@RequestPart("file") MultipartFile file,
			@Parameter(description = "Alternative text for media", required = false)
			@RequestParam(value = "altText", required = false) String altText,
			@Parameter(description = "Flag indicating if this is the primary media", required = false)
			@RequestParam(value = "isPrimary", required = false, defaultValue = "false") boolean isPrimary,
			@Parameter(description = "Type of media", required = false)
			@RequestParam(value = "mediaType", required = false, defaultValue = "IMAGE") MediaType mediaType) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		MediaRequest request = new MediaRequest();
		request.setEntityType(EntityType.RESTAURANT);
		request.setEntityId(restaurantId);
		request.setAltText(altText);
		request.setIsPrimary(isPrimary);
		request.setMediaType(mediaType);

		MediaResponse created = mediaService.createMedia(request, file, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(created));
	}

	@Operation(summary = "Update media", description = "Update details of a media item")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Media updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid media data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Media not found")
	})
	@PutMapping("/{restaurantId}/medias/{mediaId}")
	public ResponseEntity<ApiRes<MediaResponse>> updateMedia(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the media to update", required = true)
			@PathVariable UUID mediaId,
			@Parameter(description = "Updated media details", required = true)
			@Valid @RequestBody MediaRequest request) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		MediaResponse updated = mediaService.updateMedia(mediaId, request, currentUserId, userRoles);
		return ResponseEntity.ok(ApiRes.success(updated));
	}

	@Operation(summary = "Delete media", description = "Delete a media item by ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Media deleted successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Media not found")
	})
	@DeleteMapping("/{restaurantId}/medias/{mediaId}")
	public ResponseEntity<ApiRes<Void>> deleteMedia(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the media to delete", required = true)
			@PathVariable UUID mediaId) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		mediaService.deleteMedia(mediaId, currentUserId, userRoles);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Get all branches", description = "Retrieve paginated list of branches with optional filters")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "List of branches retrieved successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request parameters")
	})
	@GetMapping("/{restaurantId}/branches")
	public ResponseEntity<ApiRes<Page<BranchResponse>>> getAllBranches(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {

		int maxPageSize = 100;
		if (size > maxPageSize) size = maxPageSize;
		Page<BranchResponse> branches = branchService.findAllBranches(restaurantId, page, size);
		return ResponseEntity.ok(ApiRes.success(branches));
	}

	// --- Restaurant Staff ---

	@Operation(summary = "Assign a staff member", description = "Allows a Restaurant Owner to assign a user as a Manager or Hostess to a restaurant or specific branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Staff assigned successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied - Only Owners can manage staff"),
			@ApiResponse(responseCode = "404", description = "Restaurant or Branch not found")
	})
	@PostMapping("/{restaurantId}/staffs")
	public ResponseEntity<ApiRes<StaffResponse>> assignStaff(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@RequestBody @Valid StaffRequest request) {

		// Check if user is authenticated and is an Owner or Admin
		if (!UserContext.isValidUUID() || !(UserContext.isRoleOwner() || UserContext.isRoleAdmin() || UserContext.isRoleManager())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(ApiRes.error("Only the Restaurant Owner or Manager can manage staff assignments"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		StaffResponse response = staffService.assignStaff(restaurantId, request, currentUserId, userRoles);

		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(response));
	}

	@Operation(summary = "Get all restaurant staff", description = "Retrieve a list of all staff members (Managers, Hostesses, etc.) for a specific restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Staff list retrieved successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied")
	})
	@GetMapping("/{restaurantId}/staffs")
	public ResponseEntity<ApiRes<List<StaffResponse>>> getStaffs(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId) {

		// Owners and Admins can see the full staff list
		if (!UserContext.isValidUUID() || !(UserContext.isRoleOwner() || UserContext.isRoleAdmin() || UserContext.isRoleManager())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("Only the Restaurant Owner or Manager can manage staff assignments"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		List<StaffResponse> staffs = staffService.getStaffByRestaurant(restaurantId, currentUserId, userRoles);
		return ResponseEntity.ok(ApiRes.success(staffs));
	}

	@Operation(summary = "Remove a staff member", description = "Remove a staff member from the restaurant (unassign their role)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Staff removed successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Staff record not found")
	})
	@DeleteMapping("/{restaurantId}/staffs/{staffId}")
	public ResponseEntity<ApiRes<Void>> removeStaff(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the staff record to delete", required = true)
			@PathVariable UUID staffId) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleOwner() || UserContext.isRoleAdmin() || UserContext.isRoleManager())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("Only the Restaurant Owner or Manager can manage staff assignments"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		staffService.removeStaff(restaurantId, staffId, currentUserId, userRoles);

		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Update staff role or status", description = "Update the role (e.g., Hostess to Manager) or toggle active status of a staff member")
	@PatchMapping("/{restaurantId}/staffs/{staffId}")
	public ResponseEntity<ApiRes<StaffResponse>> updateStaff(
			@PathVariable UUID restaurantId,
			@PathVariable UUID staffId,
			@RequestBody @Valid StaffUpdateRequest request) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleOwner() || UserContext.isRoleAdmin() || UserContext.isRoleManager())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("Only the Restaurant Owner or Manager can manage staff assignments"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		StaffResponse updated = staffService.updateStaff(restaurantId, staffId, request, currentUserId, userRoles);

		return ResponseEntity.ok(ApiRes.success(updated));
	}

}