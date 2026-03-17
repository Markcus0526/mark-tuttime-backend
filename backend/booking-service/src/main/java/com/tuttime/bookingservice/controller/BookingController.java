package com.tuttime.bookingservice.controller;

import com.tuttime.bookingservice.dto.BookingRequest;
import com.tuttime.bookingservice.dto.BookingResponse;
import com.tuttime.bookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

  private final BookingService bookingService;
  private final AvailabilityService availabilityService;

  @GetMapping("/avail/{branchId}")
  public ResponseEntity<AvailabilityResponse> getAvailability(@PathVariable Long branchId, 
      AvailabilityRequest req) {
    List<Long> tables = availabilityService.getAvailableTables(branchId, req.getSlotStart(), req.getNumGuests());
    AvailabilityResponse resp = new AvailabilityResponse();
    resp.setAvailableTables(/* fetch details */ List.of());
    resp.setSlotCapacityOk(!tables.isEmpty());
    return ResponseEntity.ok(resp);
  }

  @PostMapping
  public ResponseEntity<BookingResponse> createBooking(@RequestBody BookingRequest request) {
    return ResponseEntity.ok(bookingService.createBooking(request));
  }

}
