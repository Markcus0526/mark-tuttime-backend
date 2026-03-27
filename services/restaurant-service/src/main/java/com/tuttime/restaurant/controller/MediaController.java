package com.tuttime.restaurant.controller;

import java.util.List;
import java.util.UUID;

import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.enums.EntityType;
import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.mapper.MediaMapper;
import com.tuttime.restaurant.service.MediaService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/medias")
@Tag(name = "Medias", description = "Operations for managing media (images/videos) linked to entities")
public class MediaController {

	private final MediaService mediaService;
	private final MediaMapper mediaMapper;

	@GetMapping
	@Operation(summary = "Get media by entity type and entity ID")
	public ResponseEntity<List<MediaDto>> getMediaByEntity(
			@RequestParam EntityType entityType,
			@RequestParam UUID entityId) {

		List<MediaDto> mediaDtos = mediaService.getMediaByEntity(entityType, entityId)
				.stream()
				.map(mediaMapper::mapToDto)
				.toList();

		return ResponseEntity.ok(mediaDtos);
	}

	@PostMapping
	@Operation(summary = "Add media to an entity")
	public ResponseEntity<MediaDto> create(@RequestBody MediaDto mediaDto) {
		Media created = mediaService.createMedia(mediaDto);
		return ResponseEntity.ok(mediaMapper.mapToDto(created));
	}

	@PutMapping("/{mediaId}")
	@Operation(summary = "Update media details")
	public ResponseEntity<MediaDto> updateMedia(@PathVariable UUID mediaId, @RequestBody MediaDto mediaDto) {
		Media updated = mediaService.updateMedia(mediaId, mediaDto);
		return ResponseEntity.ok(mediaMapper.mapToDto(updated));
	}

	@DeleteMapping("/{mediaId}")
	@Operation(summary = "Delete media by ID")
	public ResponseEntity<Void> deleteMedia(@PathVariable UUID mediaId) {
		mediaService.deleteMedia(mediaId);
		return ResponseEntity.noContent().build();
	}
}