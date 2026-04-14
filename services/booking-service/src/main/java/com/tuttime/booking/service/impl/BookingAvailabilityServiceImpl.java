package com.tuttime.booking.service.impl;

import com.tuttime.booking.dto.BookingRequest;
import com.tuttime.booking.repository.BookingRepository;
import com.tuttime.booking.service.BookingAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingAvailabilityServiceImpl implements BookingAvailabilityService {

	private final RedissonClient redissonClient;
	private final BookingRepository bookingRepository;

	@Override
	@Transactional
	public boolean reserveTable(BookingRequest request) {
		// 1. Create a distributed lock key for this specific table and time slot
		String lockKey = "lock:table:" + request.tableId() + ":" + request.bookingDatetime();
		RLock lock = redissonClient.getLock(lockKey);

		try {
			// 2. Try to acquire lock for 5s, hold for 10s (auto-release if app crashes)
			if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
				try {
					// 3. Double-Check DB availability inside the lock
					// Alternative using seconds
					OffsetDateTime endTime = request.bookingDatetime().plusSeconds(request.duration() * 60L);

					boolean isBooked = bookingRepository.existsOverlappingBooking(
							request.tableId(), request.bookingDatetime(), endTime);

					if (isBooked) return false;

					// 4. Save the Booking record here...
					return true;
				} finally {
					lock.unlock();
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return false;
	}
}
