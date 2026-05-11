package com.tuttime.booking.kafka;

import com.tuttime.booking.service.WaitlistService;
import com.tuttime.common.enums.BookingStatus;
import com.tuttime.common.event.kafka.event.BookingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_BOOKING_STATUS_UPDATED;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

	private final WaitlistService waitlistService;

	@KafkaListener(
			topics = TOPIC_BOOKING_STATUS_UPDATED,
			groupId = "${spring.kafka.consumer.group-id}")
	public void handleBookingStatusUpdate(BookingEvent.BookingStatusUpdatedEvent event) {
		if (event.newStatus() == BookingStatus.CANCELED || event.newStatus() == BookingStatus.NO_SHOW) {
			log.info("Table freed at branch {} due to booking {} status {}",
					event.branchId(), event.bookingId(), event.newStatus());
			waitlistService.notifyNextWaiting(event.branchId(), event.zoneId(), event.partySize());
		}
	}
}