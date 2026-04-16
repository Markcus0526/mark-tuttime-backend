package com.tuttime.booking.event.listener;

import com.tuttime.booking.entity.Booking;
import com.tuttime.booking.entity.BookingStatusHistory;
import com.tuttime.booking.event.BookingStatusChangedEvent;
import com.tuttime.booking.repository.BookingRepository;
import com.tuttime.booking.repository.BookingStatusHistoryRepository;
import com.tuttime.common.exception.InvalidRequestException;
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

	private final BookingRepository bookingRepository;
	private final BookingStatusHistoryRepository historyRepository;

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handleBookingStatusChange(BookingStatusChangedEvent event) {
		log.info("Auditing status change for booking {}: {} -> {}",
				event.bookingId(), event.oldStatus(), event.newStatus());

		Booking booking = bookingRepository.findById(event.bookingId())
				.orElseThrow(() -> new InvalidRequestException("Booking not found: " + event.bookingId()));

		// Mapping Record fields to Entity
		BookingStatusHistory history = BookingStatusHistory.builder()
				.booking(booking)
				.oldStatus(event.oldStatus())
				.newStatus(event.newStatus())
				.changedBy(event.changedBy())
				.reason(event.reason())
				.build();

		historyRepository.save(history);
	}
}
