package com.tuttime.booking.service;

import com.tuttime.booking.dto.BookingRequest;
import com.tuttime.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

public interface BookingAvailabilityService {
	boolean reserveTable(BookingRequest request);
}
