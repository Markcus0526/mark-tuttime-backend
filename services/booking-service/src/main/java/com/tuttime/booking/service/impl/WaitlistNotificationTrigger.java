package com.tuttime.booking.service.impl;

import com.tuttime.booking.entity.enums.BookingStatus;
import com.tuttime.booking.entity.enums.WaitlistStatus;
import com.tuttime.booking.event.BookingStatusChangedEvent;
import com.tuttime.booking.event.WaitlistNotificationEvent;
import com.tuttime.booking.repository.BookingRepository;
import com.tuttime.booking.repository.WaitlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitlistNotificationTrigger {

	private final BookingRepository bookingRepository;
	private final WaitlistRepository waitlistRepository;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * Triggered ONLY after the booking cancellation transaction commits.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleBookingCancellation(BookingStatusChangedEvent event) {
		if (event.newStatus() != BookingStatus.CANCELED) return;

		// 1. Fetch the booking to know WHERE the table opened up
		bookingRepository.findById(event.bookingId()).ifPresent(booking -> {

			// 2. Find the NEXT person waiting for THIS specific branch and zone
			waitlistRepository.findFirstByBranchIdAndZoneIdAndStatusOrderByJoinedAtAsc(
					booking.getBranchId(),
					booking.getZoneId(),
					WaitlistStatus.WAITING
			).ifPresent(entry -> {

				// 3. Update status to NOTIFIED so they aren't notified twice
				entry.setStatus(WaitlistStatus.NOTIFIED);
				entry.setNotifiedAt(OffsetDateTime.now());
				waitlistRepository.save(entry);

				// 4. Trigger the actual notification
				eventPublisher.publishEvent(new WaitlistNotificationEvent(
						entry.getUserId(),
						entry.getBranchId(),
						entry.getId(),
						"Good news! A table is now available in " + booking.getZoneId()
				));
			});
		});
	}

}
