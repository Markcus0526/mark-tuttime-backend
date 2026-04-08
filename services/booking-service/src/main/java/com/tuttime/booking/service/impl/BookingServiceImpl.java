package com.tuttime.booking.service.impl;

import com.tuttime.booking.dto.BookingHistoryResponse;
import com.tuttime.booking.dto.BookingRequest;
import com.tuttime.booking.dto.BookingResponse;
import com.tuttime.booking.entity.Booking;
import com.tuttime.booking.entity.enums.BookingStatus;
import com.tuttime.booking.event.BookingStatusChangedEvent;
import com.tuttime.booking.grpc.client.RestaurantGrpcClient;
import com.tuttime.booking.repository.BookingRepository;
import com.tuttime.booking.repository.BookingStatusHistoryRepository;
import com.tuttime.booking.service.BookingService;
import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

	private final BookingRepository bookingRepository;
	private final BookingStatusHistoryRepository historyRepository;
	private final RestaurantGrpcClient restaurantGrpcClient;
	private final RedissonClient redissonClient;
	private final ApplicationEventPublisher eventPublisher;

	private static final String LOCK_PREFIX = "lock:table:";

	@Override
	@Transactional
	public Booking createBooking(BookingRequest request) {
		restaurantGrpcClient.checkAvailableTableForBooking(request);

		// Calculate the end time based on requested duration
		OffsetDateTime startTime = request.bookingDatetime();
		OffsetDateTime endTime = startTime.plus(Duration.ofMinutes(request.duration()));

		// 1. Distributed Locking (Redis)
		// Lock key includes table ID and start time to prevent concurrent requests for the same slot
		String lockKey = LOCK_PREFIX + request.tableId() + ":" + startTime;
		RLock lock = redissonClient.getLock(lockKey);

		try {
			// Try to acquire lock for 5s, release after 10s if process hangs
			if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
				throw new ConflictException("This table is currently being reserved by another user.");
			}

			// 2. Overlap Check (Postgres Native Logic)
			// Checks if any existing 'active' booking overlaps with the requested [startTime, endTime]
			boolean hasOverlap = bookingRepository.existsOverlappingBooking(
					request.tableId(),
					startTime,
					endTime
			);

			if (hasOverlap) {
				throw new ConflictException("Table is already booked for the selected time range.");
			}

			// 3. Entity Creation (Using Builder pattern)
			Booking booking = Booking.builder()
					.userId(request.userId())
					.restaurantId(request.restaurantId())
					.branchId(request.branchId())
					.zoneId(request.zoneId())
					.tableId(request.tableId())
					.bookingDatetime(startTime)
					.duration(request.duration())
					.partySize(request.partySize())
					.leadTimeMinutes(request.leadTimeMinutes())
					.gracePeriodMins(request.gracePeriodMins())
					.status(BookingStatus.CREATED)
					.autoCancelled(false)
					.build();

			Booking saved = bookingRepository.save(booking);

			// 1. PLACE HERE for initial creation
			eventPublisher.publishEvent(new BookingStatusChangedEvent(
					saved.getId(),
					null,
					BookingStatus.CREATED,
					request.userId(),
					"Initial creation"
			));

			return saved;

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("System interrupted during booking process.");
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	@Override
	@Transactional
	public void updateStatus(UUID bookingId, BookingStatus newStatus, UUID actorId, String reason) {
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

		BookingStatus oldStatus = booking.getStatus();
		booking.setStatus(newStatus);

		// Save automatically updates 'updated_at' via BaseUuidEntity listeners
		bookingRepository.save(booking);

		// 2. PLACE HERE for status updates
		eventPublisher.publishEvent(new BookingStatusChangedEvent(
				bookingId,
				oldStatus,
				newStatus,
				actorId,
				reason
		));
	}

	@Override
	@Transactional
	public void confirmPayment(UUID bookingId) {
		updateStatus(bookingId, BookingStatus.PAID, null, "Payment successfully processed");
	}

	@Override
	@Transactional
	public void cancelUnpaidBooking(UUID bookingId) {
		updateStatus(bookingId, BookingStatus.CANCELED, null, "Cancelled due to payment timeout");
	}

	@Override
	@Transactional(readOnly = true)
	public List<BookingResponse> getBookingsByUser(UUID userId) {
		return bookingRepository.findAllByUserIdOrderByBookingDatetimeDesc(userId)
				.stream()
				.map(BookingResponse::fromEntity)
				.toList();
	}

	@Override
	@Transactional
	public void cancelBooking(UUID bookingId, UUID userId, String reason) {
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

		// 1. Security Check: Ensure the user owns the booking
		if (!booking.getUserId().equals(userId)) {
			throw new AccessDeniedException("You are not authorized to cancel this booking");
		}

		// 2. Business Rule: Only allow cancellation if not already COMPLETED or CANCELED
		if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELED) {
			throw new IllegalStateException("Booking cannot be cancelled in its current state");
		}

		BookingStatus oldStatus = booking.getStatus();
		booking.setStatus(BookingStatus.CANCELED);
		booking.setCancellationReason(reason != null ? reason : "Cancelled by user");

		bookingRepository.save(booking);

		// 3. Trigger the Audit and Waitlist Notification logic automatically
		eventPublisher.publishEvent(new BookingStatusChangedEvent(
				bookingId, oldStatus, BookingStatus.CANCELED, userId, reason
		));
	}

	@Override
	@Transactional(readOnly = true)
	public List<BookingHistoryResponse> getStatusHistory(UUID bookingId) {
		// 1. Verify booking exists
		if (!bookingRepository.existsById(bookingId)) {
			throw new ResourceNotFoundException("Booking not found");
		}

		// 2. Fetch all history entries for this specific booking
		return historyRepository.findAllByBookingIdOrderByChangedAtDesc(bookingId)
				.stream()
				.map(BookingHistoryResponse::fromEntity)
				.toList();
	}

}
