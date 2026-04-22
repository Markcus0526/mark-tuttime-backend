package com.tuttime.booking.task;

import com.tuttime.booking.entity.Booking;
import com.tuttime.booking.entity.BookingStatusHistory;
import com.tuttime.booking.repository.BookingRepository;
import com.tuttime.booking.repository.BookingStatusHistoryRepository;
import com.tuttime.common.enums.BookingStatus;
import com.tuttime.common.event.kafka.BookingEvent;
import com.tuttime.common.event.kafka.EventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingNoShowTask {

	private final BookingRepository bookingRepository;
	private final BookingStatusHistoryRepository historyRepository;
	private final EventProducer eventProducer;

	// Runs every minute: "0 * * * * *"
	@Scheduled(cron = "0 * * * * *")
	@Transactional
	public void processNoShows() {
		OffsetDateTime now = OffsetDateTime.now();

		// 1. Find bookings where:
		// Current Time > (Booking Time + Grace Period)
		// AND status is still 'CREATED' or 'PAID'
		List<Booking> expiredBookings = bookingRepository.findExpiredBookings(now);

		for (Booking booking : expiredBookings) {
			log.info("Marking booking {} as NO_SHOW (Reserved: {})", booking.getId(), booking.getBookingDatetime());

			BookingStatus oldStatus = booking.getStatus();
			booking.setStatus(BookingStatus.NO_SHOW);
			booking.setAutoCanceled(true);
			bookingRepository.save(booking);

			// 2. Audit Trail
			historyRepository.save(BookingStatusHistory.of(
					booking, oldStatus, null, "System auto-canceled due to guest no-show"
			));

			// 3. Notify Restaurant Service via Kafka (to free up the table)
			eventProducer.sendBookingStatusUpdatedEvent(new BookingEvent.BookingStatusUpdatedEvent(
					booking.getId(),
					booking.getTableId(),
					booking.getBranchId(),
					booking.getUserId(),
					oldStatus,
					BookingStatus.NO_SHOW,
					booking.getBookingDatetime(),
					null,
					"System auto-canceled due to guest no-show",
					OffsetDateTime.now()));
		}
	}
}

