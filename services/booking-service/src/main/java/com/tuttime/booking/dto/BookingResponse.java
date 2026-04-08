package com.tuttime.booking.dto;

import com.tuttime.booking.entity.Booking;
import com.tuttime.booking.entity.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

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
