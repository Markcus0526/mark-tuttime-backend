package com.tuttime.restaurant.controller;

import com.tuttime.restaurant.dto.BranchDto;
import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.EntityType;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.mapper.BranchMapper;
import com.tuttime.restaurant.service.BranchService;
import com.tuttime.restaurant.service.MediaService;
import com.tuttime.restaurant.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/branches")
@Tag(name = "Branches", description = "Operations related to menu branches")
public class BranchController {

    private final BranchService branchService;
    private final RestaurantService restaurantService;
    private final BranchMapper branchMapper;
    private final MediaService mediaService;

    @GetMapping
    @Operation(summary = "Get all branches")
    public List<BranchDto> getAll() {
        return branchService.getAll()
                .stream()
                .map(branchMapper::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a branch by ID")
    public ResponseEntity<BranchDto> getById(@PathVariable Long id) {
        Branch branch = branchService.getById(id);
        return ResponseEntity.ok(branchMapper.toDto(branch));
    }

    @PostMapping
    @Operation(summary = "Create a new branch")
    public ResponseEntity<BranchDto> create(@RequestBody BranchDto dto) {
        Restaurant restaurant = restaurantService.getRestaurantById(dto.getRestaurantId());
        Branch branch = branchMapper.toEntity(dto, restaurant);
        Branch saved = branchService.create(branch);
        return ResponseEntity.ok(branchMapper.toDto(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing branch")
    public ResponseEntity<BranchDto> update(@PathVariable Long id, @RequestBody BranchDto dto) {
        Restaurant restaurant = restaurantService.getRestaurantById(dto.getRestaurantId());
        Branch branch = branchMapper.toEntity(dto, restaurant);
        Branch updated = branchService.update(id, branch);
        return ResponseEntity.ok(branchMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a branch by ID")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        branchService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{branchId}/media")
    public ResponseEntity<List<MediaDto>> getMediaByBranch(@PathVariable Long branchId) {
        var medias = mediaService.getMediaByEntity(EntityType.BRANCH, branchId);
        var dtos = medias.stream().map(media -> {
            MediaDto dto = new MediaDto();
            dto.setMediaId(media.getMediaId());
            dto.setEntityType(media.getEntityType());
            dto.setEntityId(media.getEntityId());
            dto.setUrl(media.getUrl());
            dto.setAltText(media.getAltText());
            dto.setIsPrimary(media.getIsPrimary());
            dto.setMediaType(media.getMediaType());
            return dto;
        }).toList();
        return ResponseEntity.ok(dtos);
    }
}
