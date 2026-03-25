package com.tuttime.booking.controller;

import com.tuttime.booking.dto.*;
import com.tuttime.booking.service.AvailabilityService;
import com.tuttime.booking.service.BookingNotificationService;
import com.tuttime.booking.service.BookingService;
import com.tuttime.booking.service.BookingStatusHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

	private final BookingService bookingService;
	private final BookingStatusHistoryService bookingStatusHistoryService;
	private final BookingNotificationService bookingNotificationService;
//	private final AvailabilityService availabilityService;
//	private final RestaurantClient restaurantClient;

	@GetMapping
	public ResponseEntity<List<BookingDto>> getAllBookings() {
		return ResponseEntity.ok(bookingService.getAllBookings());
	}

	@GetMapping("/{id}")
	public ResponseEntity<BookingDto> getBookingById(@PathVariable UUID id) {
		return bookingService.getBookingById(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping
	public ResponseEntity<BookingDto> createBooking(@Valid @RequestBody BookingDto dto) {
		BookingDto created = bookingService.createBooking(dto);
		return ResponseEntity.ok(created);
	}

	@PutMapping("/{id}")
	public ResponseEntity<BookingDto> updateBooking(@PathVariable UUID id, @Valid @RequestBody BookingDto dto) {
		return bookingService.updateBooking(id, dto)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteBooking(@PathVariable UUID id) {
		bookingService.deleteBooking(id);
		return ResponseEntity.noContent().build();
	}

	/// Status Histories
	@GetMapping("/{id}/status-history")
	public ResponseEntity<List<BookingStatusHistoryDto>> getAllHistories(@PathVariable UUID id) {
		return ResponseEntity.ok(bookingStatusHistoryService.getAllHistories());
	}

	@PostMapping("/{id}/status-history")
	public ResponseEntity<BookingStatusHistoryDto> createHistory(
			@PathVariable UUID id,
			@RequestBody BookingStatusHistoryDto dto) {
		BookingStatusHistoryDto created = bookingStatusHistoryService.createHistory(dto);
		return ResponseEntity.ok(created);
	}

	/// Notifications
	@GetMapping("/{id}/notifications")
	public ResponseEntity<List<BookingNotificationDto>> getAllNotifications(@PathVariable UUID id) {
		return ResponseEntity.ok(bookingNotificationService.getAllNotifications());
	}

	@PostMapping("/{id}/notifications")
	public ResponseEntity<BookingNotificationDto> createNotification(
			@PathVariable UUID id,
			@RequestBody BookingNotificationDto dto) {
		BookingNotificationDto created = bookingNotificationService.createNotification(dto);
		return ResponseEntity.ok(created);
	}

//	@GetMapping("/avail/{branchId}")
//	public ResponseEntity<AvailabilityResponse> getAvailability(@PathVariable Long branchId,
//	                                                            @RequestBody AvailabilityRequest req) {
//		List<TableAvailabilityDto> tables = restaurantClient.getAvailableTables(branchId, req.getSlotStart()).stream()
//				.filter(TableAvailabilityDto::isAvailable)
//				.collect(java.util.stream.Collectors.toList());
//		AvailabilityResponse resp = new AvailabilityResponse();
//		resp.setAvailableTables(tables);
//		resp.setSlotCapacityOk(!tables.isEmpty());
//		return ResponseEntity.ok(resp);
//	}

}
