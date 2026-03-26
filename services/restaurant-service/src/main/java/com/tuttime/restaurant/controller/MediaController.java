package com.tuttime.restaurant.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.mapper.MediaMapper;
import com.tuttime.restaurant.service.MediaService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/medias")
@Tag(name = "Medias", description = "Operations related to menu medias")
public class MediaController {

	private final MediaService mediaService;
	private final MediaMapper mediaMapper;

	public MediaController(MediaService mediaService, MediaMapper mediaMapper) {
		this.mediaService = mediaService;
		this.mediaMapper = mediaMapper;
	}

	@GetMapping
	@Operation(summary = "Get all mediaes")
	public List<MediaDto> getAll() {
		return mediaService.getAll().stream()
				.map(mediaMapper::toDto)
				.collect(Collectors.toList());
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a media by ID")
	public ResponseEntity<MediaDto> getById(@PathVariable UUID id) {
		Media media = mediaService.getById(id);
		return ResponseEntity.ok(mediaMapper.toDto(media));
	}

	@PostMapping
	@Operation(summary = "Create a new media")
	public ResponseEntity<MediaDto> create(@RequestBody MediaDto dto) {
		Media media = mediaMapper.toEntity(dto);
		Media saved = mediaService.create(media);
		return ResponseEntity.ok(mediaMapper.toDto(saved));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update an existing media")
	public ResponseEntity<MediaDto> update(@PathVariable UUID id, @RequestBody MediaDto dto) {
		Media mediaDetails = mediaMapper.toEntity(dto);
		Media updated = mediaService.update(id, mediaDetails);
		return ResponseEntity.ok(mediaMapper.toDto(updated));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a media by ID")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		mediaService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
