package com.tuttime.booking.controller;

import com.tuttime.booking.dto.BookingHistoryResponse;
import com.tuttime.booking.dto.BookingRequest;
import com.tuttime.booking.dto.BookingResponse;
import com.tuttime.booking.entity.Booking;
import com.tuttime.booking.service.BookingService;
import com.tuttime.common.dto.ApiRes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking Management", description = "Endpoints for creating and managing restaurant table reservations")
public class BookingController {

	private final BookingService bookingService;

	@PostMapping
	@Operation(
			summary = "Create a table reservation",
			description = "Attempts to acquire a distributed lock on a table and create a pending booking if no overlaps exist."
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "201",
					description = "Booking created successfully",
					content = @Content(schema = @Schema(implementation = BookingResponse.class))
			),
			@ApiResponse(
					responseCode = "400",
					description = "Invalid request data (validation failed)"
			),
			@ApiResponse(
					responseCode = "409",
					description = "Table already booked or currently locked for this slot"
			)
	})
	public ResponseEntity<ApiRes<BookingResponse>> createBooking(@Valid @RequestBody BookingRequest request) {
		Booking saved = bookingService.createBooking(request);

		// Convert Entity to Record using our static factory method
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiRes.success(BookingResponse.fromEntity(saved)));
	}

	@GetMapping("/user/{userId}")
	@Operation(summary = "Get user bookings", description = "Retrieves all bookings (active and past) for a specific user.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Success",
					content = @Content(schema = @Schema(implementation = BookingResponse.class)))
	})
	public ResponseEntity<ApiRes<List<BookingResponse>>> getUserBookings(@PathVariable UUID userId) {
		List<BookingResponse> data = bookingService.getBookingsByUser(userId);
		return ResponseEntity.ok(ApiRes.success(data));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Cancel a booking", description = "Sets the booking status to CANCELED. Does not delete the record.")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Successfully cancelled"),
			@ApiResponse(responseCode = "404", description = "Booking not found"),
			@ApiResponse(responseCode = "403", description = "Not authorized to cancel this booking")
	})
	public ResponseEntity<Void> cancelBooking(
			@PathVariable UUID id,
			@RequestParam UUID userId, // In production, get this from SecurityContext (JWT)
			@RequestParam(required = false) String reason) {

		bookingService.cancelBooking(id, userId, reason);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Get booking audit trail", description = "Returns a timeline of all status changes for a specific booking.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Success"),
			@ApiResponse(responseCode = "404", description = "Booking not found")
	})
	public ResponseEntity<ApiRes<List<BookingHistoryResponse>>> getBookingHistory(@PathVariable UUID id) {
		List<BookingHistoryResponse> data = bookingService.getStatusHistory(id);
		return ResponseEntity.ok(ApiRes.success(data));
	}
}
