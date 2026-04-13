package com.tuttime.restaurant.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.entity.enums.MediaType;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/restaurants/medias")
@Tag(name = "Medias", description = "Operations related to media of restaurants, branches, zones, tables")
public class MediaController {

	private final MediaService mediaService;

	@Operation(summary = "Get medias", description = "Retrieve all media items associated with a restaurant, branch, zone, table ...")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Media retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@GetMapping("/{entityId}/{mediaType}")
	public ResponseEntity<ApiRes<List<MediaDto.MediaResponse>>> getMediaForRestaurant(
			@Parameter(description = "UUID of the entity", required = true)
			@PathVariable UUID entityId,
			@Parameter(description = "Type of the entity")
			@PathVariable MediaType mediaType) {

		List<MediaDto.MediaResponse> medias = mediaService.getMediaByEntity(entityId, mediaType);
		return ResponseEntity.ok(ApiRes.success(medias));
	}

	@Operation(summary = "Add media, ", description = "Upload a new media item (image/video/svg) for a restaurant, branch, zone, table ...")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Media added successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid media data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiRes<MediaDto.MediaResponse>> addMediaToRestaurant(
			@Parameter(description = "details to create", required = true)
			@Valid @RequestBody MediaDto.MediaRequest request,
			@RequestPart("file") MultipartFile file) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		MediaDto.MediaResponse created = mediaService.createMedia(request, file, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(created));
	}

	@Operation(summary = "Update media", description = "Update details of a media item")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Media updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid media data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Media not found")
	})
	@PutMapping("/{mediaId}")
	public ResponseEntity<ApiRes<MediaDto.MediaResponse>> updateMedia(
			@Parameter(description = "UUID of the media to delete", required = true)
			@PathVariable UUID mediaId,
			@Parameter(description = "Updated media details", required = true)
			@Valid @RequestBody MediaDto.MediaRequest request,
			@RequestPart("file") MultipartFile file) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		MediaDto.MediaResponse updated = mediaService.updateMedia(mediaId, request, file, currentUserId, userRoles);
		return ResponseEntity.ok(ApiRes.success(updated));
	}

	@Operation(summary = "Delete media", description = "Delete a media item by ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Media deleted successfully"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Media not found")
	})
	@DeleteMapping("/{mediaId}")
	public ResponseEntity<ApiRes<Void>> deleteMedia(
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

	// --- Map SVG (Restaurant & Branch) ---

	@Operation(summary = "Get map Canvas", description = "Retrieve the inline Canvas map for a branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Branch map Canvas retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Branch or map not found")
	})
	@GetMapping("/maps/{entityId}")
	public ResponseEntity<ApiRes<String>> getMapCanvas(
			@Parameter(description = "UUID of the entity", required = true)
			@PathVariable UUID entityId) {

		String svgContent = mediaService.getMapCanvas(entityId);
		return ResponseEntity.ok(ApiRes.success(svgContent));
	}

	@Operation(summary = "Set/update branch map Canvas", description = "Upload or update the inline Canvas map for a branch")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Branch map Canvas set/updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid Canvas content"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Branch not found")
	})
	@PostMapping("/maps/{entityId}")
	public ResponseEntity<ApiRes<Void>> setMapCanvas(
			@Parameter(description = "UUID of the entity", required = true)
			@PathVariable UUID entityId,
			@Parameter(description = "Canvas XML content", required = true)
			@RequestBody String svgContent) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner() || UserContext.isRoleManager())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific media"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		mediaService.setOrUpdateMapCanvas(entityId, svgContent, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(null));
	}
}