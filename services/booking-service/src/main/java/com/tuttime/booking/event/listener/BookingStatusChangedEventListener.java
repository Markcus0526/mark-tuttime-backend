package com.tuttime.booking.event.listener;

import com.tuttime.booking.entity.BookingStatusHistory;
import com.tuttime.booking.event.BookingStatusChangedEvent;
import com.tuttime.booking.repository.BookingStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingStatusChangedEventListener {

	private final BookingStatusHistoryRepository historyRepository;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handleBookingStatusChange(BookingStatusChangedEvent event) {
		log.info("Auditing status change for booking {}: {} -> {}",
				event.bookingId(), event.oldStatus(), event.newStatus());

		// Mapping Record fields to Entity
		BookingStatusHistory history = BookingStatusHistory.builder()
				.bookingId(event.bookingId())
				.oldStatus(event.oldStatus())
				.newStatus(event.newStatus())
				.changedBy(event.changedBy())
				.reason(event.reason())
				.build();

		historyRepository.save(history);
	}
}
