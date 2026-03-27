package com.tuttime.restaurant.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.tuttime.restaurant.dto.BranchDto;
import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.enums.EntityType;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.mapper.BranchMapper;
import com.tuttime.restaurant.service.BranchService;
import com.tuttime.restaurant.service.MediaService;
import com.tuttime.restaurant.service.RestaurantService;

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
import lombok.AllArgsConstructor;

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
	public ResponseEntity<BranchDto> getById(@PathVariable UUID id) {
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
	public ResponseEntity<BranchDto> update(@PathVariable UUID id, @RequestBody BranchDto dto) {
		Restaurant restaurant = restaurantService.getRestaurantById(dto.getRestaurantId());
		Branch branch = branchMapper.toEntity(dto, restaurant);
		Branch updated = branchService.update(id, branch);
		return ResponseEntity.ok(branchMapper.toDto(updated));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a branch by ID")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		branchService.delete(id);
		return ResponseEntity.noContent().build();
	}

	// --- Branch Schedules ---

//	@Operation(summary = "Get schedules for a branch", description = "Retrieve all schedule entries for a given branch.")
//	@ApiResponses({
//			@ApiResponse(responseCode = "200", description = "Schedules retrieved successfully"),
//			@ApiResponse(responseCode = "404", description = "Branch not found")
//	})
//	@GetMapping("/branches/{branchId}/schedules")
//	public ResponseEntity&lt;List&lt;ScheduleResponse&gt;&gt; getBranchSchedules(@PathVariable UUID branchId) {
//		List&lt;ScheduleResponse&gt; schedules = scheduleService.getSchedulesByBranchId(branchId);
//		return ResponseEntity.ok(schedules);
//	}
//
//	@Operation(summary = "Create a schedule for a branch", description = "Add a new schedule entry to a branch.")
//	@ApiResponses({
//			@ApiResponse(responseCode = "201", description = "Schedule created successfully"),
//			@ApiResponse(responseCode = "400", description = "Invalid schedule data"),
//			@ApiResponse(responseCode = "404", description = "Branch not found")
//	})
//	@PostMapping("/branches/{branchId}/schedules")
//	public ResponseEntity&lt;ScheduleResponse&gt; createBranchSchedule(@PathVariable UUID branchId,
//	                                                                   @Valid @RequestBody ScheduleRequest request) {
//		ScheduleResponse created = scheduleService.createScheduleForBranch(branchId, request);
//		return ResponseEntity.status(HttpStatus.CREATED).body(created);
//	}
//
//	@Operation(summary = "Update a branch schedule", description = "Update a schedule entry of a branch.")
//	@ApiResponses({
//			@ApiResponse(responseCode = "200", description = "Schedule updated successfully"),
//			@ApiResponse(responseCode = "400", description = "Invalid schedule data"),
//			@ApiResponse(responseCode = "404", description = "Schedule or branch not found")
//	})
//	@PutMapping("/branches/{branchId}/schedules/{scheduleId}")
//	public ResponseEntity&lt;ScheduleResponse&gt; updateBranchSchedule(@PathVariable UUID branchId,
//	                                                                   @PathVariable UUID scheduleId,
//	                                                                   @Valid @RequestBody ScheduleRequest request) {
//		ScheduleResponse updated = scheduleService.updateSchedule(scheduleId, null, branchId, request);
//		return ResponseEntity.ok(updated);
//	}
//
//	@Operation(summary = "Delete a branch schedule", description = "Delete a schedule entry of a branch.")
//	@ApiResponses({
//			@ApiResponse(responseCode = "204", description = "Schedule deleted successfully"),
//			@ApiResponse(responseCode = "404", description = "Schedule or branch not found")
//	})
//	@DeleteMapping("/branches/{branchId}/schedules/{scheduleId}")
//	public ResponseEntity&lt;Void&gt; deleteBranchSchedule(@PathVariable UUID branchId,
//	                                                       @PathVariable UUID scheduleId) {
//		scheduleService.deleteSchedule(scheduleId, null, branchId);
//		return ResponseEntity.noContent().build();
//	}
//
//	@GetMapping("/{branchId}/media")
//	public ResponseEntity<List<MediaDto>> getMediaByBranch(@PathVariable UUID branchId) {
//		var medias = mediaService.getMediaByEntity(EntityType.BRANCH, branchId);
//		var dtos = medias.stream().map(media -> {
//			MediaDto dto = new MediaDto();
//			dto.setMediaId(media.getMediaId());
//			dto.setEntityType(media.getEntityType());
//			dto.setEntityId(media.getEntityId());
//			dto.setUrl(media.getUrl());
//			dto.setAltText(media.getAltText());
//			dto.setIsPrimary(media.getIsPrimary());
//			dto.setMediaType(media.getMediaType());
//			return dto;
//		}).toList();
//		return ResponseEntity.ok(dtos);
//	}
}
