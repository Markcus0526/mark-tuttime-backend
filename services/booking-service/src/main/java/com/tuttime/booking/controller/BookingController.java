package com.tuttime.booking.controller;

import com.tuttime.booking.dto.*;
import com.tuttime.booking.entity.Booking;
import com.tuttime.booking.entity.enums.BookingStatus;
import com.tuttime.booking.service.BookingService;
import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking Management", description = "Core service for table reservations and status tracking")
public class BookingController {

	private final BookingService bookingService;

//	@GetMapping("/availability")
//	@Operation(summary = "Check table availability", description = "Checks if a specific table or zone is available for a time slot.")
//	public ResponseEntity<ApiRes<Boolean>> checkAvailability(
//			@RequestParam UUID branchId,
//			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTime,
//			@RequestParam Integer partySize) {
//		return ResponseEntity.ok(ApiRes.success(bookingService.isAvailable(branchId, dateTime, partySize)));
//	}

	@GetMapping("/restaurant/{branchId}/slots")
	public ResponseEntity<ApiRes<BookingSetupResponse>> getAvailableSlots(
			@PathVariable UUID branchId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) OffsetDateTime date,
			@RequestParam Integer partySize) {

		BookingSetupResponse data = bookingService.getBookingSetupData(branchId, date, partySize);
		return ResponseEntity.ok(ApiRes.success(data));
	}

	@PostMapping
	@Operation(summary = "Initiate a booking", description = "Creates a PENDING booking and locks the table in Redis.")
	public ResponseEntity<ApiRes<BookingResponse>> createBooking(@Valid @RequestBody BookingRequest request) {
		Booking saved = bookingService.createBooking(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiRes.success(BookingResponse.fromEntity(saved)));
	}

	@GetMapping("/me")
	@Operation(summary = "Get my bookings", description = "Returns the booking history and upcoming reservations for the current user.")
	public ResponseEntity<ApiRes<List<BookingResponse>>> getMyBookings() {
		List<BookingResponse> data = bookingService.getBookingsByUser(UUID.fromString(UserContext.getUserId()));
		return ResponseEntity.ok(ApiRes.success(data));
	}

	@GetMapping("/restaurant/{restaurantId}")
	@Operation(summary = "Admin: Get restaurant bookings", description = "Allows restaurant staff to view all bookings for their branch.")
	public ResponseEntity<ApiRes<List<BookingResponse>>> getRestaurantBookings(
			@PathVariable UUID restaurantId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime date) {
		List<BookingResponse> data = bookingService.getBookingsByRestaurant(restaurantId, date);
		return ResponseEntity.ok(ApiRes.success(data));
	}

	@PostMapping("/{id}/check-in")
	@Operation(summary = "Check-in guest", description = "Updates booking status to COMPLETED/ARRIVED when guest arrives.")
	public ResponseEntity<ApiRes<BookingResponse>> checkIn(
			@PathVariable UUID id) {
		Booking updated = bookingService.updateStatus(
				id,
				BookingStatus.COMPLETED,
				UUID.fromString(UserContext.getUserId()),
				"Customer checked in at restaurant"); // Logic from booking_status_histories SQL
		return ResponseEntity.ok(ApiRes.success(BookingResponse.fromEntity(updated)));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Cancel a booking", description = "Sets status to CANCELED and records the reason.")
	public ResponseEntity<Void> cancelBooking(
			@PathVariable UUID id,
			@RequestParam UUID userId,
			@RequestParam(required = false) String reason) {
		bookingService.cancelBooking(id, userId, reason);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/history")
	@Operation(summary = "Get audit trail", description = "Returns timeline of status changes from booking_status_histories table.")
	public ResponseEntity<ApiRes<List<BookingHistoryResponse>>> getBookingHistory(@PathVariable UUID id) {
		List<BookingHistoryResponse> data = bookingService.getStatusHistory(id);
		return ResponseEntity.ok(ApiRes.success(data));
	}
}
