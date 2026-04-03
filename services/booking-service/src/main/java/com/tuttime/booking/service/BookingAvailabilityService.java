package com.tuttime.booking.service;

import com.tuttime.booking.dto.BookingRequest;

public interface BookingAvailabilityService {
	boolean reserveTable(BookingRequest request);
}
