package com.tuttime.booking.scheduler;

import com.tuttime.booking.entity.enums.BookingStatus;
import com.tuttime.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCleanupScheduler {

	private final BookingRepository bookingRepository;

	/**
	 * Runs every 60 seconds.
	 * Finds bookings created > 15 mins ago that are still in 'CREATED' status and cancels them.
	 */
	@Scheduled(fixedRate = 60000)
	@Transactional
	public void cleanupUnpaidBookings() {
		OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(15);

		int deletedCount = bookingRepository.cancelExpiredBookings(
				BookingStatus.CREATED,
				BookingStatus.CANCELED,
				cutoff
		);

		if (deletedCount > 0) {
			log.info("Cleanup task: Cancelled {} expired unpaid bookings.", deletedCount);
		}
	}
}
