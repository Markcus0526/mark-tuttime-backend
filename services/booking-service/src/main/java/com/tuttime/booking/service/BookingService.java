package com.tuttime.booking.service;

import com.tuttime.booking.dto.BookingDto;
import com.tuttime.booking.dto.BookingHistoryDto;
import com.tuttime.booking.dto.BookingSetupDto;
import com.tuttime.booking.dto.WaitlistDto;
import com.tuttime.booking.entity.Booking;
import com.tuttime.booking.entity.enums.WaitlistStatus;
import com.tuttime.common.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingService {

	/**
	 * Creates a new booking with distributed locking for concurrency control.
	 */
	BookingDto.BookingResponse createBooking(BookingDto.BookingRequest request, UUID currentUserId, String userRoles);

	BookingDto.BookingResponse editBooking(UUID bookingId, BookingDto.BookingUpdateRequest request,
	                                       UUID currentUserId, String userRoles);

	BookingDto.BookingDetailResponse getBooking(UUID bookingId, UUID currentUserId, String userRoles);

	BookingDto.BookingResponse updateStatus(UUID bookingId, BookingStatus newStatus, String reason,
	                                        UUID currentUserId, String userRoles);

	void deleteBooking(UUID bookingId, UUID userId, String reason, UUID currentUserId, String userRoles);

	List<BookingDto.BookingDetailResponse> getBookingsByUser(UUID userId);

	List<BookingHistoryDto.BookingHistoryResponse> getStatusHistory(UUID bookingId);

	Page<BookingDto.BookingDetailResponse> getBookingsByBranch(
			UUID branchId,
			OffsetDateTime fromDate,
			OffsetDateTime toDate,
			BookingStatus status,
			Pageable pageable,
			UUID currentUserId,
			String userRoles);

	BookingSetupDto.BookingSetupResponse getAvailableSlots(UUID branchId, OffsetDateTime date, int partySize,
	                                                       UUID currentUserId);

	void markAsPaid(UUID paymentId, UUID bookingId, UUID staffOrSystemId, String paymentRef);

	Booking createBookingFromWaitlist(BookingDto.BookingRequest request, UUID currentUserId, String userRoles);

	// ------------------- Waitlist Service ---------------------------------
	WaitlistDto.WaitlistResponse addToWaitlist(WaitlistDto.WaitlistRequest request, UUID currentUserId, String userRoles);

	WaitlistDto.WaitlistResponse getWaitlistEntry(UUID id, UUID currentUserId, String userRoles);

	void leaveWaitlist(UUID id, UUID currentUserId, String userRoles);

	void updateWaitlistStatus(UUID id, WaitlistStatus newStatus, UUID currentUserId, String userRoles);

	void notifyNextWaiting(UUID branchId, UUID zoneId, int partySize);

	BookingDto.BookingResponse claimWaitlistEntry(UUID entryId, BookingDto.BookingRequest bookingRequest,
	                                              UUID currentUserId, String userRoles);

}
