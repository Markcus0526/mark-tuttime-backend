package com.tuttime.restaurant.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.entity.enums.MediaType;
import com.tuttime.restaurant.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
@RequestMapping("/api/v1/restaurant-medias")
@Tag(name = "6.Restaurant Medias", description = "Operations related to media of restaurants, branches, zones, tables")
public class MediaController {

	private final MediaService mediaService;

	@Operation(summary = "Get medias", description = "Retrieve all media items associated with a restaurant, branch, zone, table ...")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Media retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MediaDto.MediaResponse.class))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@GetMapping({
			"/{entityId}/{mediaType}", // Matches both
			"/{entityId}"              // Matches when mediaType is missing
	})
	public ResponseEntity<ApiRes<List<MediaDto.MediaResponse>>> getMediaForRestaurant(
			@Parameter(description = "UUID of the entity")
			@PathVariable UUID entityId,
			@Parameter(description = "Type of the entity")
			@PathVariable(required = false) MediaType mediaType) {

		List<MediaDto.MediaResponse> medias = mediaService.getMediaByEntity(entityId, mediaType);
		return ResponseEntity.ok(ApiRes.success(medias));
	}

	@Operation(summary = "Add media, ", description = "Upload a new media item (image/video/svg) for a restaurant, branch, zone, table ...")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Media added successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MediaDto.MediaResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid media data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@PostMapping(
			value = "",
			consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
	)
	public ResponseEntity<ApiRes<MediaDto.MediaResponse>> addMediaToRestaurant(
			@Parameter(
					description = "Media metadata in JSON format",
					required = true,
					content = @Content(mediaType = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
			)
			@RequestPart("mediaRequest") @Valid MediaDto.MediaRequest request,
			@RequestPart("file") MultipartFile file) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		MediaDto.MediaResponse created = mediaService.createMedia(request, file, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(created));
	}

	@Operation(summary = "Update media", description = "Update details of a media item")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Media updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MediaDto.MediaResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid media data"),
			@ApiResponse(responseCode = "403", description = "Access denied"),
			@ApiResponse(responseCode = "404", description = "Media not found")
	})
	@PutMapping(
			value = "/{mediaId}",
			consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
	)
	public ResponseEntity<ApiRes<MediaDto.MediaResponse>> updateMedia(
			@Parameter(description = "UUID of the media to delete", required = true)
			@PathVariable UUID mediaId,
			@Parameter(
					description = "Media metadata in JSON format",
					required = true,
					content = @Content(mediaType = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
			)
			@RequestPart("mediaRequest") @Valid MediaDto.MediaRequest request,
			@RequestPart("file") MultipartFile file) {

		validateAdminOrOwnerOrManager();
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

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		mediaService.deleteMedia(mediaId, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
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
			throw new ForbiddenException("Access forbidden: You do not manage this method");
		}
	}

	private void validateAdminOrOwner() {
		if (!UserContext.isValidUUID()
				|| !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			throw new ForbiddenException("Access forbidden: You do not manage this method");
		}
	}

	private void validateAdminOrOwnerOrManager() {
		if (!UserContext.isValidUUID()
				|| !(UserContext.isRoleAdmin() || UserContext.isRoleOwner() || UserContext.isRoleManager())) {
			throw new ForbiddenException("Access forbidden: You do not manage this method");
		}
	}
}