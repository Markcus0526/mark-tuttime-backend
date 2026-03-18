package com.tuttime.analytics.controller;

import com.tuttime.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
  private final AnalyticsService analyticsService;

  @GetMapping("/occupancy/{restaurantId}")
  public Map<String, Object> getOccupancy(@PathVariable Long restaurantId, @RequestParam LocalDateTime since) {
    // Query recent bookings for occupancy
    return Map.of("occupancyRate", 0.75, "forecast", "High demand expected", "avgPrepTime", "25min");
  }
}
