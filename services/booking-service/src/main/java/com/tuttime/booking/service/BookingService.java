package com.tuttime.booking.service;

import com.tuttime.booking.dto.BookingDto;
import com.tuttime.booking.dto.BookingHistoryDto;
import com.tuttime.booking.dto.BookingSetupDto;
import com.tuttime.common.enums.BookingStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingService {

	/**
	 * Creates a new booking with distributed locking for concurrency control.
	 */
	BookingDto.BookingResponse createBooking(BookingDto.BookingRequest request, UUID currentUserId, String userRoles);

	BookingDto.BookingResponse editBooking(UUID bookingId, BookingDto.BookingRequest request,
	                                       UUID currentUserId, String userRoles);

	BookingDto.BookingDetailResponse getBooking(UUID bookingId, UUID currentUserId, String userRoles);

	BookingDto.BookingResponse updateStatus(UUID bookingId, BookingStatus newStatus, String reason,
	                                        UUID currentUserId, String userRoles);

	void deleteBooking(UUID bookingId, UUID userId, String reason, UUID currentUserId, String userRoles);

	List<BookingDto.BookingDetailResponse> getBookingsByUser(UUID userId);

	List<BookingHistoryDto.BookingHistoryResponse> getStatusHistory(UUID bookingId);

	List<BookingDto.BookingDetailResponse> getBookingsByBranch(UUID branchId, OffsetDateTime date,
	                                                           UUID currentUserId, String userRoles);

	BookingSetupDto.BookingSetupResponse getAvailableSlots(UUID branchId, OffsetDateTime date, int partySize,
	                                                       UUID currentUserId);

	void markAsPaid(UUID paymentId, UUID bookingId, UUID staffOrSystemId, String paymentRef);

}
