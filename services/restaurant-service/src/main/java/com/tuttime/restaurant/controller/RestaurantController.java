package com.tuttime.restaurant.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.tuttime.restaurant.dto.*;
import com.tuttime.restaurant.enums.EntityType;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.enums.MediaType;
import com.tuttime.restaurant.mapper.RestaurantMapper;
import com.tuttime.restaurant.service.MediaService;
import com.tuttime.restaurant.service.RestaurantService;

import com.tuttime.restaurant.service.ScheduleService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/restaurants")
@Tag(name = "Restaurants", description = "Operations related to menu restaurants")
public class RestaurantController {

	private final RestaurantService restaurantService;
	private final ScheduleService scheduleService;
	private final MediaService mediaService;

	// --- Restaurant CRUD ---

	@Operation(summary = "Get all restaurants", description = "Retrieve a paginated list of restaurants with optional filters.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "List of restaurants retrieved successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request parameters")
	})
	@GetMapping
	public ResponseEntity<List<RestaurantResponse>> findRestaurantAll(
			@RequestParam(required = false) UUID ownerUserId,
			@RequestParam(required = false) Double minRating,
			@RequestParam(required = false) Double maxRating,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		List<RestaurantResponse> restaurants = restaurantService.findRestaurantAll(ownerUserId, minRating, maxRating, page, size);
		return ResponseEntity.ok(restaurants);
	}

	@Operation(summary = "Get a restaurant by ID", description = "Retrieve detailed information about a restaurant by its ID.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Restaurant retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@GetMapping("/{restaurantId}")
	public ResponseEntity<RestaurantResponse> getRestaurantById(@PathVariable UUID restaurantId) {
		RestaurantResponse restaurant = restaurantService.getRestaurantById(restaurantId);
		return ResponseEntity.ok(restaurant);
	}

	@Operation(summary = "Create a new restaurant", description = "Add a new restaurant to the system.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Restaurant created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input data")
	})
	@PostMapping
	public ResponseEntity<RestaurantResponse> createRestaurant(@Valid @RequestBody RestaurantRequest request) {
		RestaurantResponse created = restaurantService.createRestaurant(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@Operation(summary = "Update a restaurant", description = "Update an existing restaurant's details.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Restaurant updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid update data"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@PutMapping("/{restaurantId}")
	public ResponseEntity<RestaurantResponse> updateRestaurant(@PathVariable UUID restaurantId,
	                                                                 @Valid @RequestBody RestaurantRequest request) {
		RestaurantResponse updated = restaurantService.updateRestaurant(restaurantId, request);
		return ResponseEntity.ok(updated);
	}

	@Operation(summary = "Delete a restaurant", description = "Remove a restaurant and all its associated data.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Restaurant deleted successfully"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@DeleteMapping("/{restaurantId}")
	public ResponseEntity<Void> deleteRestaurant(@PathVariable UUID restaurantId) {
		restaurantService.deleteRestaurant(restaurantId);
		return ResponseEntity.noContent().build();
	}

	// --- Restaurant Schedules ---

	@Operation(summary = "Get schedules for a restaurant", description = "Retrieve all schedule entries for a given restaurant.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Schedules retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@GetMapping("/{restaurantId}/schedules")
	public ResponseEntity<List<ScheduleResponse>> getSchedules(@PathVariable UUID restaurantId) {
		List<ScheduleResponse> schedules = scheduleService.getSchedulesByRestaurantId(restaurantId);
		return ResponseEntity.ok(schedules);
	}

	@Operation(summary = "Add a schedule to a restaurant", description = "Create a new schedule entry for a restaurant.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Schedule created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid schedule data"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@PostMapping("/{restaurantId}/schedules")
	public ResponseEntity<ScheduleResponse> addSchedule(@PathVariable UUID restaurantId,
	                                                          @Valid @RequestBody ScheduleRequest request) {
		ScheduleResponse created = scheduleService.createScheduleForRestaurant(restaurantId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@Operation(summary = "Update a restaurant schedule", description = "Update a specific schedule entry for a restaurant.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Schedule updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid schedule data"),
			@ApiResponse(responseCode = "404", description = "Schedule or restaurant not found")
	})
	@PutMapping("/{restaurantId}/schedules/{scheduleId}")
	public ResponseEntity<ScheduleResponse> updateSchedule(@PathVariable UUID restaurantId,
	                                                             @PathVariable UUID scheduleId,
	                                                             @Valid @RequestBody ScheduleRequest request) {
		ScheduleResponse updated = scheduleService.updateSchedule(scheduleId, restaurantId, null, request);
		return ResponseEntity.ok(updated);
	}

	@Operation(summary = "Delete a restaurant schedule", description = "Delete a specific schedule entry from a restaurant.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Schedule deleted successfully"),
			@ApiResponse(responseCode = "404", description = "Schedule or restaurant not found")
	})
	@DeleteMapping("/{restaurantId}/schedules/{scheduleId}")
	public ResponseEntity<Void> deleteSchedule(@PathVariable UUID restaurantId,
	                                                 @PathVariable UUID scheduleId) {
		scheduleService.deleteSchedule(scheduleId, restaurantId, null);
		return ResponseEntity.noContent().build();
	}

	// --- Restaurant Media ---

	@Operation(summary = "Get media for a restaurant", description = "Retrieve all media items associated with a restaurant.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Media retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@GetMapping("/{restaurantId}/medias")
	public ResponseEntity<List<MediaResponse>> getMediaForRestaurant(@PathVariable UUID restaurantId) {
		List<MediaResponse> medias = mediaService.getMediaByEntity(EntityType.RESTAURANT, restaurantId);
		return ResponseEntity.ok(medias);
	}

	@Operation(summary = "Add media to a restaurant", description = "Upload a new media item (image/video) for a restaurant.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Media added successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid media data"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	@PostMapping("/{restaurantId}/medias")
	public ResponseEntity<MediaResponse> addMediaToRestaurant(@PathVariable UUID restaurantId,
	                                                                @Valid @RequestBody MediaRequest request) {
		request.setEntityType("RESTAURANT");
		request.setEntityId(restaurantId);
		MediaResponse created = mediaService.createMedia(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@Operation(summary = "Update media", description = "Update details of a media item.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Media updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid media data"),
			@ApiResponse(responseCode = "404", description = "Media not found")
	})
	@PutMapping("/medias/{mediaId}")
	public ResponseEntity<MediaResponse> updateMedia(@PathVariable UUID mediaId,
	                                                       @Valid @RequestBody MediaRequest request) {
		MediaResponse updated = mediaService.updateMedia(mediaId, request);
		return ResponseEntity.ok(updated);
	}

	@Operation(summary = "Delete media", description = "Delete a media item by ID.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Media deleted successfully"),
			@ApiResponse(responseCode = "404", description = "Media not found")
	})
	@DeleteMapping("/medias/{mediaId}")
	public ResponseEntity<Void> deleteMedia(@PathVariable UUID mediaId) {
		mediaService.deleteMedia(mediaId);
		return ResponseEntity.noContent().build();
	}
}
