package com.tuttime.menu.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.menu.dto.MediaDto;
import com.tuttime.menu.entity.enums.MediaType;
import com.tuttime.menu.service.MediaService;
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
@RequestMapping("/api/v1/menu-medias")
@Tag(name = "6.Menu Medias", description = "Operations related to media of menu category, item")
public class MediaController {

	private final MediaService mediaService;

	@Operation(summary = "Get medias", description = "Retrieve all media associated with an entity ID (Category, Item, etc.)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Media retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Entity not found")
	})
	@GetMapping("/{entityId}")
	public ResponseEntity<ApiRes<List<MediaDto.MediaResponse>>> getMedia(
			@PathVariable UUID entityId,
			@Parameter(description = "Optional filter by IMAGE or VIDEO")
			@RequestParam(required = false) MediaType mediaType) {

		List<MediaDto.MediaResponse> medias = mediaService.getMediaByEntity(entityId, mediaType);
		return ResponseEntity.ok(ApiRes.success(medias));
	}

	@Operation(summary = "Add media", description = "Upload a new media item. Required: 'mediaRequest' (JSON) and 'file' (Binary)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Media added successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid media data")
	})
	@PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiRes<MediaDto.MediaResponse>> addMedia(
			@Parameter(content = @Content(mediaType = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)) // Forces Swagger to send as JSON
			@RequestPart("mediaRequest") @Valid MediaDto.MediaRequest request,
			@RequestPart("file") MultipartFile file) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		MediaDto.MediaResponse created = mediaService.createMedia(request, file, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(created));
	}

	@Operation(summary = "Update media", description = "Update media metadata or replace the file. File is optional.")
	@PutMapping(value = "/{mediaId}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiRes<MediaDto.MediaResponse>> updateMedia(
			@PathVariable UUID mediaId,
			@RequestPart("mediaRequest") @Valid MediaDto.MediaRequest request,
			@RequestPart(value = "file", required = false) MultipartFile file) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		MediaDto.MediaResponse updated = mediaService.updateMedia(mediaId, request, file, currentUserId, userRoles);
		return ResponseEntity.ok(ApiRes.success(updated));
	}

	@Operation(summary = "Delete media")
	@DeleteMapping("/{mediaId}")
	public ResponseEntity<ApiRes<Void>> deleteMedia(@PathVariable UUID mediaId) {
		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		mediaService.deleteMedia(mediaId, currentUserId, userRoles);
		return ResponseEntity.noContent().build(); // 204 No Content is standard for delete
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