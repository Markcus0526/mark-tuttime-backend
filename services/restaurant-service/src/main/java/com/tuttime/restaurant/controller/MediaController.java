package com.tuttime.restaurant.controller;

import com.tuttime.restaurant.dto.BranchDto;
import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.mapper.BranchMapper;
import com.tuttime.restaurant.mapper.MediaMapper;
import com.tuttime.restaurant.service.BranchService;
import com.tuttime.restaurant.service.MediaService;
import com.tuttime.restaurant.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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
    public ResponseEntity<MediaDto> getById(@PathVariable Long id) {
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
    public ResponseEntity<MediaDto> update(@PathVariable Long id, @RequestBody MediaDto dto) {
        Media mediaDetails = mediaMapper.toEntity(dto);
        Media updated = mediaService.update(id, mediaDetails);
        return ResponseEntity.ok(mediaMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a media by ID")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        mediaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
