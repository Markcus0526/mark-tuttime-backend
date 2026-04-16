package com.tuttime.booking.dto;

import com.tuttime.booking.entity.Booking;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class BookingDto {

	/**
	 * Immutable DTO for creating a new booking.
	 */
	public record BookingRequest(
			@NotNull(message = "User ID is required")
			UUID userId,

			@NotNull(message = "Table ID is required")
			UUID tableId,

			@NotNull(message = "Booking time is required")
			@Future(message = "Booking must be in the future")
			OffsetDateTime bookingDatetime,

			@NotNull(message = "Duration is required")
			@Min(value = 15, message = "Minimum duration is 15 minutes")
			Integer duration,

			@NotNull(message = "Party size is required")
			@Min(value = 1, message = "Party size must be at least 1")
			Integer partySize,

			// If a user books a table for 7:00 PM with a leadTimeMinutes of 15,
			// the system effectively marks that table as "in use" starting from 6:45 PM to ensure it's ready for the guest's arrival.
			Integer leadTimeMinutes,

			// If the booking is at 7:00 PM, the gracePeriodMins of 15 means the booking is technically valid until 7:15 PM.
			@Min(value = 0)
			Integer gracePeriodMins,

			List<PreOrderItemRequest> preOrderItems
	) {
		public record PreOrderItemRequest(
				@NotNull UUID menuItemId,
				@Min(1) Integer quantity,
				List<UUID> modifierIds, // For extras like "extra cheese"
				BigDecimal price, // For initial client-side total calculation
				String notes
		) {}

		// Compact constructor for default values
		public BookingRequest {
			if (leadTimeMinutes == null) leadTimeMinutes = 0;
			if (gracePeriodMins == null) gracePeriodMins = 15;
		}
	}

	/**
	 * Immutable response DTO for client applications.
	 */
	public record BookingResponse(
			UUID id,
			UUID userId,
			UUID tableId,
			OffsetDateTime bookingDatetime,
			Integer duration,
			Integer partySize,
			Integer gracePeriodMins,
			String status,
			String cancellationReason,
			BigDecimal totalAmount,
			String paymentUrl // Optional: To redirect user to a gateway like Stripe/Apple Pay
	) {
		/**
		 * Static factory method to map from Entity to Record.
		 * Keeps your Controller code clean.
		 */
		public static BookingResponse fromEntity(Booking entity) {
			return new BookingResponse(
					entity.getId(),
					entity.getUserId(),
					entity.getTableId(),
					entity.getBookingDatetime(),
					entity.getDuration(),
					entity.getPartySize(),
					entity.getGracePeriodMins(),
					entity.getStatus().name(),
					entity.getCancellationReason(),
					entity.getTotalAmount(),
					null // Populate with actual gateway link if needed
			);
		}
	}
}
