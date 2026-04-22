package com.tuttime.booking.scheduler;

import com.tuttime.booking.entity.Booking;
import com.tuttime.booking.entity.BookingStatusHistory;
import com.tuttime.booking.repository.BookingRepository;
import com.tuttime.booking.repository.BookingStatusHistoryRepository;
import com.tuttime.common.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCleanupScheduler {

	private final BookingRepository bookingRepository;
	private final BookingStatusHistoryRepository historyRepository;

	/**
	 * Runs every 60 seconds. TODO
	 * Finds bookings created > 1440 mins ago that are still in 'CREATED' status and cancels them.
	 */
	@Scheduled(fixedRate = 60000)
	@Transactional
	public void cleanupUnpaidBookings() {
		OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(1440);
		String reason = "System automatic cancellation: Unpaid after 24 hours";
		String systemUser = "SYSTEM";

		// 1. Fetch bookings that are about to be canceled
		List<Booking> expiredBookings = bookingRepository.findAllByStatusAndCreatedAtBefore(
				BookingStatus.CREATED,
				cutoff
		);

		if (expiredBookings.isEmpty()) {
			return;
		}

		// 2. Update statuses and record history
		expiredBookings.forEach(booking -> {
			BookingStatus oldStatus = booking.getStatus();
			booking.setStatus(BookingStatus.CANCELED);
			booking.setCancellationReason(reason);

			// Save history entry for each
			historyRepository.save(BookingStatusHistory.of(
					booking,
					oldStatus,
					null,
					reason
			));
		});

		// 3. Save all changes (JPA handles the updates automatically via Dirty Checking)
		log.info("Cleanup task: Canceled {} expired unpaid bookings.", expiredBookings.size());
	}

}
