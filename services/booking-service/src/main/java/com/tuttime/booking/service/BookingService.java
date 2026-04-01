package com.tuttime.booking.service;

import com.tuttime.booking.dto.BookingHistoryResponse;
import com.tuttime.booking.dto.BookingResponse;
import com.tuttime.booking.entity.Booking;
import com.tuttime.booking.dto.BookingRequest;
import com.tuttime.booking.entity.enums.BookingStatus;

import java.util.List;
import java.util.UUID;

public interface BookingService {

	/**
	 * Creates a new booking with distributed locking for concurrency control.
	 */
	Booking createBooking(BookingRequest request);

	/**
	 * Updates the status of an existing booking and triggers domain events.
	 */
	void updateStatus(UUID bookingId, BookingStatus newStatus, UUID actorId, String reason);

	/**
	 * Confirms payment for a booking (usually called by Payment Service).
	 */
	void confirmPayment(UUID bookingId);

	/**
	 * Cancels an unpaid booking if it exceeds the payment timeout.
	 */
	void cancelUnpaidBooking(UUID bookingId);

	public List<BookingResponse> getBookingsByUser(UUID userId);

	public void cancelBooking(UUID bookingId, UUID userId, String reason);

	public List<BookingHistoryResponse> getStatusHistory(UUID bookingId);
}
