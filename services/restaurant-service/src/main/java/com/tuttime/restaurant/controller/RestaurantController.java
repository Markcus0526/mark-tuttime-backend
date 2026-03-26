package com.tuttime.restaurant.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.dto.RestaurantDto;
import com.tuttime.restaurant.entity.EntityType;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.mapper.RestaurantMapper;
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
@RequestMapping("/api/v1/restaurants")
@Tag(name = "Restaurants", description = "Operations related to menu restaurants")
public class RestaurantController {

	private final RestaurantService service;
	private final RestaurantMapper mapper;
	private final MediaService mediaService;

	@GetMapping
	@Operation(summary = "Get all restaurants")
	public ResponseEntity<List<RestaurantDto>> getAll() {
		return ResponseEntity.ok(service.getAllRestaurants()
				.stream()
				.map(mapper::toDto)
				.collect(Collectors.toList()));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a restaurant by ID")
	public ResponseEntity<RestaurantDto> getById(@PathVariable UUID id) {
		Restaurant restaurant = service.getRestaurantById(id);
		return ResponseEntity.ok(mapper.toDto(restaurant));
	}

	@PostMapping
	@Operation(summary = "Create a new restaurant")
	public ResponseEntity<RestaurantDto> create(@RequestBody RestaurantDto dto) {
		Restaurant restaurant = mapper.toEntity(dto);
		Restaurant saved = service.createRestaurant(restaurant);
		return ResponseEntity.ok(mapper.toDto(saved));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update an existing restaurant")
	public ResponseEntity<RestaurantDto> update(@PathVariable UUID id, @RequestBody RestaurantDto dto) {
		Restaurant restaurantDetails = mapper.toEntity(dto);
		Restaurant updated = service.updateRestaurant(id, restaurantDetails);
		return ResponseEntity.ok(mapper.toDto(updated));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a restaurant by ID")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		service.deleteRestaurant(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{restaurantId}/media")
	public ResponseEntity<List<MediaDto>> getMediaByRestaurant(@PathVariable UUID restaurantId) {
		var medias = mediaService.getMediaByEntity(EntityType.RESTAURANT, restaurantId);
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
