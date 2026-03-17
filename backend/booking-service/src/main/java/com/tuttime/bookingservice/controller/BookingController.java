package com.tuttime.bookingservice.controller;

import com.tuttime.bookingservice.dto.AvailabilityRequest;
import com.tuttime.bookingservice.dto.AvailabilityResponse;
import com.tuttime.bookingservice.dto.BookingRequest;
import com.tuttime.bookingservice.dto.BookingResponse;
import com.tuttime.bookingservice.dto.TableAvailabilityDto;
import com.tuttime.bookingservice.service.AvailabilityService;
import com.tuttime.bookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tuttime.bookingservice.feign.RestaurantClient;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

  private final BookingService bookingService;
  private final AvailabilityService availabilityService;
  private final RestaurantClient restaurantClient;

@GetMapping("/avail/{branchId}")
  public ResponseEntity<AvailabilityResponse> getAvailability(@PathVariable Long branchId, 
      @RequestBody AvailabilityRequest req) {
    List<TableAvailabilityDto> tables = restaurantClient.getAvailableTables(branchId, req.getSlotStart()).stream()
      .filter(TableAvailabilityDto::isAvailable)
      .collect(java.util.stream.Collectors.toList());
    AvailabilityResponse resp = new AvailabilityResponse();
    resp.setAvailableTables(tables);
    resp.setSlotCapacityOk(!tables.isEmpty());
    return ResponseEntity.ok(resp);
  }

  @PostMapping
  public ResponseEntity<BookingResponse> createBooking(@RequestBody BookingRequest request) {
    return ResponseEntity.ok(bookingService.createBooking(request));
  }

}
