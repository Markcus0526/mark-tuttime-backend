package com.tuttime.restaurant.controller;

import com.tuttime.restaurant.dto.RestaurantRequest;
import com.tuttime.restaurant.dto.TableAvailabilityRequest;
import com.tuttime.restaurant.dto.TableAvailabilityResponse;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

  private final RestaurantService restaurantService;

  @PostMapping
  public ResponseEntity<Restaurant> createRestaurant(@RequestBody RestaurantRequest request) {
    return ResponseEntity.ok(restaurantService.createRestaurant(request));
  }

  @GetMapping("/tables/available")
  public ResponseEntity<List<TableAvailabilityResponse>> getAvailableTables(@ModelAttribute TableAvailabilityRequest request) {
    return ResponseEntity.ok(restaurantService.getAvailableTables(request));
  }

}
