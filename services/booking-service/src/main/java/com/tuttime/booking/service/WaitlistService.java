package com.tuttime.booking.service;

import com.tuttime.booking.dto.BookingDto;
import com.tuttime.booking.dto.WaitlistDto;
import com.tuttime.booking.entity.enums.WaitlistStatus;

import java.util.UUID;

public interface WaitlistService {
	WaitlistDto.WaitlistResponse addToWaitlist(WaitlistDto.WaitlistRequest request, UUID currentUserId, String userRoles);

	WaitlistDto.WaitlistResponse getWaitlistEntry(UUID id, UUID currentUserId, String userRoles);

	void leaveWaitlist(UUID id, UUID currentUserId, String userRoles);

	void updateWaitlistStatus(UUID id, WaitlistStatus newStatus, UUID currentUserId, String userRoles);

	void notifyNextWaiting(UUID branchId, UUID zoneId, int partySize);

	BookingDto.BookingResponse claimWaitlistEntry(UUID entryId, BookingDto.BookingRequest bookingRequest,
	                                              UUID currentUserId, String userRoles);
}
