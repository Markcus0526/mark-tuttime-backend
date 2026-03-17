package com.tuttime.restaurantservice.controller;

import com.tuttime.restaurantservice.dto.RestaurantRequest;
import com.tuttime.restaurantservice.dto.TableAvailabilityRequest;
import com.tuttime.restaurantservice.dto.TableAvailabilityResponse;
import com.tuttime.restaurantservice.entity.Restaurant;
import com.tuttime.restaurantservice.service.RestaurantService;
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
    return ResponseEntity.ok((List<TableAvailabilityResponse>) restaurantService.getAvailableTables(request));
  }

}
