package com.tuttime.booking.service;

import com.tuttime.booking.dto.BookingDto;

public interface BookingAvailabilityService {
	boolean reserveTable(BookingDto.BookingRequest request);
}
