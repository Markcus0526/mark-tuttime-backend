package com.tuttime.booking.event;

import com.tuttime.booking.entity.enums.BookingStatus;

import java.util.UUID;

public record BookingStatusChangedEvent(
		UUID bookingId,
		BookingStatus oldStatus,
		BookingStatus newStatus,
		UUID changedBy,
		String reason
) {}
