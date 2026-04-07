package com.tuttime.restaurant.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.restaurant.dto.*;
import com.tuttime.restaurant.entity.enums.EntityType;
import com.tuttime.restaurant.entity.enums.MediaType;
import com.tuttime.restaurant.service.BranchService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/restaurants/{restaurantId}/branches")
@Tag(name = "Branches", description = "Operations related to branches, schedules & media")
public class BranchController {

	private final BranchService branchService;
	private final RestaurantService restaurantService;
	private final ScheduleService scheduleService;
	private final MediaService mediaService;

	// --- Branch CRUD ---

	@Operation(summary = "Get all branches", description = "Retrieve paginated list of branches with optional filters")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "List of branches retrieved successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request parameters")
	})
	@GetMapping
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

	@Operation(summary = "Get a branch by ID", description = "Retrieve detailed information about a branch by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Branch retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Branch not found")
	})
	@GetMapping("/{branchId}")
	public ResponseEntity<ApiRes<BranchResponse>> getBranchById(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch to retrieve", required = true)
			@PathVariable UUID branchId) {
		BranchResponse branch = branchService.getBranchById(branchId);
		return ResponseEntity.ok(ApiRes.success(branch));
	}

	@Operation(summary = "Create a new branch", description = "Add a new branch to a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Branch created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input data"),
			@ApiResponse(responseCode = "403", description = "Access denied")
	})
	@PostMapping
	public ResponseEntity<ApiRes<BranchResponse>> createBranch(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "Branch creation payload", required = true)
			@Valid @RequestBody BranchRequest request) {
		restaurantService.validateRestaurantOwner(restaurantId);

		BranchResponse created = branchService.createBranch(restaurantId, request);
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
	public ResponseEntity<ApiRes<BranchResponse>> updateBranch(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch to update", required = true)
			@PathVariable UUID branchId,
			@Parameter(description = "Updated branch details", required = true)
			@Valid @RequestBody BranchRequest request) {
		branchService.validateBranchManager(branchId, restaurantId);

		BranchResponse updated = branchService.updateBranch(branchId, request);
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
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch to delete", required = true)
			@PathVariable UUID branchId) {
		branchService.validateBranchManager(branchId, restaurantId);

		branchService.deleteBranch(branchId);
		return ResponseEntity.noContent().build();
	}

	// --- Branch Map (Media SVG) ---

	@Operation(summary = "Get branch map SVG", description = "Retrieve the inline SVG map for a branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Branch map SVG retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Branch or map not found")
	})
	@GetMapping("/{branchId}/map")
	public ResponseEntity<ApiRes<String>> getBranchMapSvg(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId) {
		String svgContent = mediaService.getPrimarySvgMap(EntityType.BRANCH, branchId);
		return ResponseEntity.ok(ApiRes.success(svgContent));
	}

	@Operation(summary = "Set/update branch map SVG", description = "Upload or update the inline SVG map for a branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Branch map SVG set/updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid SVG content"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Branch not found")
	})
	@PostMapping("/{branchId}/map")
	public ResponseEntity<ApiRes<Void>> setBranchMapSvg(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId,
			@Parameter(description = "SVG XML content", required = true)
			@RequestBody String svgContent) {
		branchService.validateBranchManager(branchId, restaurantId);

		mediaService.setOrUpdateSvgMap(EntityType.BRANCH, branchId, svgContent);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(null));
	}

	// --- Branch Schedules ---

	@Operation(summary = "Get schedules for a branch", description = "Retrieve all schedule entries for a branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Schedules retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Branch not found")
	})
	@GetMapping("/{branchId}/schedules")
	public ResponseEntity<ApiRes<List<ScheduleResponse>>> getSchedules(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId) {
		List<ScheduleResponse> schedules = scheduleService.getSchedulesByBranchId(branchId);
		return ResponseEntity.ok(ApiRes.success(schedules));
	}

	@Operation(summary = "Add a schedule to a branch", description = "Create a new schedule entry for a branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Schedule created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid schedule data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Branch not found")
	})
	@PostMapping("/{branchId}/schedules")
	public ResponseEntity<ApiRes<ScheduleResponse>> addSchedule(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId,
			@Parameter(description = "Schedule details to create", required = true)
			@Valid @RequestBody ScheduleRequest request) {
		branchService.validateBranchManager(branchId, restaurantId);

		ScheduleResponse created = scheduleService.createScheduleForBranch(branchId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(created));
	}

	@Operation(summary = "Update a branch schedule", description = "Update a specific schedule entry for a branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Schedule updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid schedule data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Schedule or branch not found")
	})
	@PutMapping("/{branchId}/schedules/{scheduleId}")
	public ResponseEntity<ApiRes<ScheduleResponse>> updateSchedule(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId,
			@Parameter(description = "UUID of the schedule to update", required = true)
			@PathVariable UUID scheduleId,
			@Parameter(description = "Updated schedule details", required = true)
			@Valid @RequestBody ScheduleRequest request) {
		branchService.validateBranchManager(branchId, restaurantId);

		ScheduleResponse updated = scheduleService.updateBranchSchedule(scheduleId, branchId, request);
		return ResponseEntity.ok(ApiRes.success(updated));
	}

	@Operation(summary = "Delete a branch schedule", description = "Delete a specific schedule entry from a branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Schedule deleted successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Schedule or branch not found")
	})
	@DeleteMapping("/{branchId}/schedules/{scheduleId}")
	public ResponseEntity<ApiRes<Void>> deleteSchedule(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId,
			@Parameter(description = "UUID of the schedule to delete", required = true)
			@PathVariable UUID scheduleId) {
		branchService.validateBranchManager(branchId, restaurantId);

		scheduleService.deleteBranchSchedule(scheduleId, branchId);
		return ResponseEntity.noContent().build();
	}

	// --- Branch Media (Images, Videos, SVGs) ---

	@Operation(summary = "Get media for a branch", description = "Retrieve all media items associated with a branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Media retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Branch not found")
	})
	@GetMapping("/{branchId}/medias")
	public ResponseEntity<ApiRes<List<MediaResponse>>> getMediaForBranch(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId) {
		List<MediaResponse> medias = mediaService.getMediaByEntity(EntityType.BRANCH, branchId);
		return ResponseEntity.ok(ApiRes.success(medias));
	}

	@Operation(summary = "Add media to a branch", description = "Upload a new media item (image/video/svg) for a branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Media added successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid media data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Branch not found")
	})
	@PostMapping("/{branchId}/medias")
	public ResponseEntity<ApiRes<MediaResponse>> addMediaToBranch(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable UUID restaurantId,
			@Parameter(description = "UUID of the branch", required = true)
			@PathVariable UUID branchId,
			@Parameter(description = "Media file to upload", required = true)
			@RequestParam("file") MultipartFile file,
			@Parameter(description = "Alternative text for media", required = false)
			@RequestParam(value = "altText", required = false) String altText,
			@Parameter(description = "Flag indicating if this is the primary media", required = false)
			@RequestParam(value = "isPrimary", required = false, defaultValue = "false") boolean isPrimary,
			@Parameter(description = "Type of media", required = false)
			@RequestParam(value = "mediaType", required = false, defaultValue = "IMAGE") MediaType mediaType) {
		branchService.validateBranchManager(branchId, restaurantId);

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
		request.setEntityType(EntityType.BRANCH);
		request.setEntityId(branchId);
		request.setAltText(altText);
		request.setIsPrimary(isPrimary);
		request.setMediaType(mediaType);

		MediaResponse created = mediaService.createMedia(request, file);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(created));
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