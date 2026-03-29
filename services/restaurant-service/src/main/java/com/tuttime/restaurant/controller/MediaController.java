package com.tuttime.restaurant.controller;

import java.util.List;
import java.util.UUID;

import com.tuttime.common.dto.ApiRes;
import com.tuttime.restaurant.dto.MediaRequest;
import com.tuttime.restaurant.dto.MediaResponse;
import com.tuttime.restaurant.enums.EntityType;
import com.tuttime.restaurant.enums.MediaType;
import com.tuttime.restaurant.service.MediaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Media", description = "Operations related to media items linked to restaurant entities")
public class MediaController {

	private final MediaService mediaService;

	// --- Get media by entity (restaurant, branch, zone, table) ---

	@Operation(summary = "Get media items by entity", description = "Retrieve all media items associated with a specific entity")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Media retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Entity not found")
	})
	@GetMapping("/{entityType}/{entityId}/medias")
	@PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'MANAGER', 'HOSTESS', 'USER', 'GUEST')")
	public ResponseEntity<ApiRes<List<MediaResponse>>> getMediaByEntity(
			@Parameter(description = "Entity type (RESTAURANT, BRANCH, ZONE, TABLE)", required = true)
			@PathVariable String entityType,
			@Parameter(description = "UUID of the entity", required = true)
			@PathVariable UUID entityId) {

		EntityType typeEnum = EntityType.valueOf(entityType.toUpperCase());
		List<MediaResponse> medias = mediaService.getMediaByEntity(typeEnum, entityId);
		return ResponseEntity.ok(ApiRes.success(medias));
	}

	// --- Add media to an entity ---

	@Operation(summary = "Add media to an entity", description = "Upload a new media item (image/video/svg) for an entity")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Media added successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid media data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Entity not found")
	})
	@PostMapping("/{entityType}/{entityId}/medias")
	@PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwnerByEntity(#entityType, #entityId, @securityUtil.getCurrentUserId())")
	public ResponseEntity<ApiRes<MediaResponse>> addMediaToEntity(
			@Parameter(description = "Entity type (RESTAURANT, BRANCH, ZONE, TABLE)", required = true)
			@PathVariable String entityType,
			@Parameter(description = "UUID of the entity", required = true)
			@PathVariable UUID entityId,
			@Parameter(description = "Media file to upload", required = true)
			@RequestParam("file") MultipartFile file,
			@Parameter(description = "Alternative text for media", required = false)
			@RequestParam(value = "altText", required = false) String altText,
			@Parameter(description = "Flag indicating if this is the primary media", required = false)
			@RequestParam(value = "isPrimary", required = false, defaultValue = "false") boolean isPrimary,
			@Parameter(description = "Type of media", required = false)
			@RequestParam(value = "mediaType", required = false, defaultValue = "IMAGE") MediaType mediaType) {

		EntityType typeEnum = EntityType.valueOf(entityType.toUpperCase());

		MediaRequest request = new MediaRequest();
		request.setEntityType(typeEnum.name());
		request.setEntityId(entityId);
		request.setAltText(altText);
		request.setIsPrimary(isPrimary);
		request.setMediaType(mediaType);

		MediaResponse created = mediaService.createMedia(request, file);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(created));
	}

	// --- Update media details ---

	@Operation(summary = "Update media details", description = "Update metadata/details of a media item")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Media updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid media data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Media not found")
	})
	@PutMapping("/medias/{mediaId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiRes<MediaResponse>> updateMedia(
			@Parameter(description = "UUID of the media to update", required = true)
			@PathVariable UUID mediaId,
			@Parameter(description = "Updated media details", required = true)
			@Valid @RequestBody MediaRequest request) {
		MediaResponse updated = mediaService.updateMedia(mediaId, request);
		return ResponseEntity.ok(ApiRes.success(updated));
	}

	// --- Delete media ---

	@Operation(summary = "Delete media", description = "Delete a media item by its ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Media deleted successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Media not found")
	})
	@DeleteMapping("/medias/{mediaId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteMedia(
			@Parameter(description = "UUID of the media to delete", required = true)
			@PathVariable UUID mediaId) {
		mediaService.deleteMedia(mediaId);
		return ResponseEntity.noContent().build();
	}
}