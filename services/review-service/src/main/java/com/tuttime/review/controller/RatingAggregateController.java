package com.tuttime.review.controller;

import com.tuttime.review.dto.RatingAggregateDto;
import com.tuttime.review.service.RatingAggregateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rating-aggregates")
@Validated
@Tag(name = "Rating Aggregates", description = "Operations for restaurant rating aggregates")
public class RatingAggregateController {

	private final RatingAggregateService ratingAggregateService;

	public RatingAggregateController(RatingAggregateService ratingAggregateService) {
		this.ratingAggregateService = ratingAggregateService;
	}

	@Operation(summary = "Get rating aggregate by restaurant ID")
	@GetMapping("/{restaurantId}")
	public ResponseEntity<RatingAggregateDto> getByRestaurantId(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable @NotNull UUID restaurantId) {
		RatingAggregateDto dto = ratingAggregateService.getByRestaurantId(restaurantId);
		return ResponseEntity.ok(dto);
	}

	@Operation(summary = "Create or update rating aggregate")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public RatingAggregateDto createOrUpdate(
			@Parameter(description = "Rating aggregate data", required = true)
			@Valid @RequestBody RatingAggregateDto dto) {
		return ratingAggregateService.createOrUpdate(dto);
	}

	@Operation(summary = "Delete rating aggregate by restaurant ID")
	@DeleteMapping("/{restaurantId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(
			@Parameter(description = "UUID of the restaurant rating aggregate to delete", required = true)
			@PathVariable @NotNull UUID restaurantId) {
		ratingAggregateService.delete(restaurantId);
	}
}
