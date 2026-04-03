package com.tuttime.booking.service;

import com.tuttime.booking.dto.WaitlistDto;
import com.tuttime.booking.entity.enums.WaitlistStatus;

import java.util.UUID;

public interface WaitlistService {
	WaitlistDto.WaitlistResponse addToWaitlist(WaitlistDto.WaitlistRequest request);

	WaitlistDto.WaitlistResponse getWaitlistEntry(UUID id);

	void leaveWaitlist(UUID id);

	void updateWaitlistStatus(UUID id, WaitlistStatus newStatus);
}
