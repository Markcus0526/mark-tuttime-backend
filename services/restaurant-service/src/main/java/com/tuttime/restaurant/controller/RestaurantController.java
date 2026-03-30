package com.tuttime.restaurant.controller;

import com.tuttime.common.dto.ApiRes;
import com.tuttime.restaurant.dto.*;
import com.tuttime.restaurant.enums.EntityType;
import com.tuttime.restaurant.enums.MediaType;
import com.tuttime.restaurant.service.MediaService;
import com.tuttime.restaurant.service.RestaurantService;
import com.tuttime.restaurant.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

	// --- Restaurant CRUD ---

	@Operation(summary = "Get all restaurants", description = "Retrieve paginated list of restaurants with optional filters")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "List of restaurants retrieved successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request parameters")
	})
	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'MANAGER', 'HOSTESS', 'USER', 'GUEST')")
	public ResponseEntity<ApiRes<Page<RestaurantResponse>>> getAllRestaurants(
			@RequestParam(required = false) UUID ownerUserId,
			@RequestParam(required = false) Double minRating,
			@RequestParam(required = false) Double maxRating,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		int maxPageSize = 100;
		if (size > maxPageSize) size = maxPageSize;
		Page<RestaurantResponse> restaurants = restaurantService.findRestaurantAll(ownerUserId, minRating, maxRating, page, size);
		return ResponseEntity.ok(ApiRes.success(restaurants));
	}

	@Operation(summary = "Get a restaurant by ID", description = "Retrieve detailed information about a restaurant by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Restaurant retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@GetMapping("/{restaurantId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'MANAGER', 'HOSTESS', 'USER', 'GUEST')")
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
	@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
	public ResponseEntity<ApiRes<RestaurantResponse>> createRestaurant(
			@Parameter(description = "Restaurant creation payload", required = true)
			@Valid @RequestBody RestaurantRequest request) {
		RestaurantResponse created = restaurantService.createRestaurant(request);
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
	@PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(#restaurantId, @securityUtil.getCurrentUserId())")
	public ResponseEntity<ApiRes<RestaurantResponse>> updateRestaurant(
			@Parameter(description = "UUID of the restaurant to update", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "Updated restaurant details", required = true)
			@Valid @RequestBody RestaurantRequest request) {
		RestaurantResponse updated = restaurantService.updateRestaurant(restaurantId, request);
		return ResponseEntity.ok(ApiRes.success(updated));
	}

	@Operation(summary = "Delete a restaurant", description = "Remove a restaurant and all its associated data")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Restaurant deleted successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@DeleteMapping("/{restaurantId}")
	@PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(#restaurantId, @securityUtil.getCurrentUserId())")
	public ResponseEntity<Void> deleteRestaurant(
			@Parameter(description = "UUID of the restaurant to delete", required = true)
			@PathVariable UUID restaurantId) {
		restaurantService.deleteRestaurant(restaurantId);
		return ResponseEntity.noContent().build();
	}

	// --- Restaurant Search by near ---

	@Operation(summary = "Find restaurants near a location", description = "Retrieve restaurants within a radius (in meters) of given latitude and longitude")
	@GetMapping("/near")
	@PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'MANAGER', 'HOSTESS', 'USER', 'GUEST')")
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
	@PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'MANAGER', 'HOSTESS', 'USER', 'GUEST')")
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
	@PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(#restaurantId, @securityUtil.getCurrentUserId()) or hasRole('MANAGER')")
	public ResponseEntity<ApiRes<ScheduleResponse>> addSchedule(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "Schedule details to create", required = true)
			@Valid @RequestBody ScheduleRequest request) {
		ScheduleResponse created = scheduleService.createScheduleForRestaurant(restaurantId, request);
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
	@PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(#restaurantId, @securityUtil.getCurrentUserId()) or hasRole('MANAGER')")
	public ResponseEntity<ApiRes<ScheduleResponse>> updateSchedule(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the schedule to update", required = true)
			@PathVariable UUID scheduleId,
			@Parameter(description = "Updated schedule details", required = true)
			@Valid @RequestBody ScheduleRequest request) {
		ScheduleResponse updated = scheduleService.updateSchedule(scheduleId, restaurantId, null, request);
		return ResponseEntity.ok(ApiRes.success(updated));
	}

	@Operation(summary = "Delete a restaurant schedule", description = "Delete a specific schedule entry from a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Schedule deleted successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Schedule or restaurant not found")
	})
	@DeleteMapping("/{restaurantId}/schedules/{scheduleId}")
	@PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(#restaurantId, @securityUtil.getCurrentUserId()) or hasRole('MANAGER')")
	public ResponseEntity<Void> deleteSchedule(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the schedule to delete", required = true)
			@PathVariable UUID scheduleId) {
		scheduleService.deleteSchedule(scheduleId, restaurantId, null);
		return ResponseEntity.noContent().build();
	}

	// --- Restaurant Media (Images, Videos, SVGs) ---

	@Operation(summary = "Get media for a restaurant", description = "Retrieve all media items associated with a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Media retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@GetMapping("/{restaurantId}/medias")
	@PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'MANAGER', 'HOSTESS', 'USER', 'GUEST')")
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
//	@PostMapping("/{restaurantId}/medias")
	@PostMapping(value = "/{restaurantId}/medias", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(#restaurantId, @securityUtil.getCurrentUserId())")
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

		// Validate file size (e.g., max 5MB)
		long maxFileSize = 5 * 1024 * 1024; // 5 MB
		if (file.getSize() > maxFileSize) {
			throw new IllegalArgumentException("File size exceeds the maximum allowed limit of 5 MB");
		}

		// Validate file type using Apache Tika
		Tika tika = new Tika();
		String detectedType;
		try {
			detectedType = tika.detect(file.getInputStream());
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not detect file type");
		}

		// Allowed MIME types based on mediaType parameter
		List<String> allowedMimeTypes = switch (mediaType) {
			case IMAGE -> List.of("image/jpeg", "image/png", "image/gif", "image/svg+xml");
			case VIDEO -> List.of("video/mp4", "video/webm", "video/ogg");
			case SVG -> List.of("image/svg+xml");
			default -> throw new IllegalArgumentException("Unsupported media type");
		};

		if (!allowedMimeTypes.contains(detectedType)) {
			throw new IllegalArgumentException("Invalid file type: " + detectedType);
		}

		// Optional: further validate SVG content if mediaType == SVG
		if (mediaType == MediaType.SVG) {
			try {
				String svgContent = new String(file.getBytes(), StandardCharsets.UTF_8);
				if (!isValidSvg(svgContent)) {
					throw new IllegalArgumentException("Invalid or unsafe SVG content");
				}
			} catch (IOException e) {
				throw new IllegalArgumentException("Failed to read SVG content");
			}
		}

		MediaRequest request = new MediaRequest();
		request.setEntityType(EntityType.RESTAURANT);
		request.setEntityId(restaurantId);
		request.setAltText(altText);
		request.setIsPrimary(isPrimary);
		request.setMediaType(mediaType);

		MediaResponse created = mediaService.createMedia(request, file);
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
	@PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(#restaurantId, @securityUtil.getCurrentUserId())")
	public ResponseEntity<ApiRes<MediaResponse>> updateMedia(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the media to update", required = true)
			@PathVariable UUID mediaId,
			@Parameter(description = "Updated media details", required = true)
			@Valid @RequestBody MediaRequest request) {
		MediaResponse updated = mediaService.updateMedia(mediaId, request);
		return ResponseEntity.ok(ApiRes.success(updated));
	}

	@Operation(summary = "Delete media", description = "Delete a media item by ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Media deleted successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Media not found")
	})
	@DeleteMapping("/{restaurantId}/medias/{mediaId}")
	@PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(#restaurantId, @securityUtil.getCurrentUserId())")
	public ResponseEntity<Void> deleteMedia(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the media to delete", required = true)
			@PathVariable UUID mediaId) {
		mediaService.deleteMedia(mediaId);
		return ResponseEntity.noContent().build();
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
}