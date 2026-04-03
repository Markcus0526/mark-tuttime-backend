package com.tuttime.booking.dto;

import com.tuttime.booking.entity.Booking;
import com.tuttime.booking.entity.enums.BookingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Immutable response DTO for client applications.
 */
public record BookingResponse(
		UUID id,
		UUID userId,
		UUID restaurantId,
		UUID branchId,
		UUID zoneId,
		UUID tableId,
		BookingStatus status,
		OffsetDateTime bookingDatetime,
		Integer duration,
		Integer partySize,
		Integer gracePeriodMins,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt,
		String cancellationReason
) {
	/**
	 * Static factory method to map from Entity to Record.
	 * Keeps your Controller code clean.
	 */
	public static BookingResponse fromEntity(Booking entity) {
		return new BookingResponse(
				entity.getId(),
				entity.getUserId(),
				entity.getRestaurantId(),
				entity.getBranchId(),
				entity.getZoneId(),
				entity.getTableId(),
				entity.getStatus(),
				entity.getBookingDatetime(),
				entity.getDuration(),
				entity.getPartySize(),
				entity.getGracePeriodMins(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getCancellationReason()
		);
	}
}
