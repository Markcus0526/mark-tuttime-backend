package com.tuttime.order.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.enums.BookingStatus;
import com.tuttime.common.event.BookingEvent;
import com.tuttime.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import static com.tuttime.common.event.EventProducer.TOPIC_BOOKING_STATUS_UPDATED;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

	private final ObjectMapper objectMapper;
	private final OrderService orderService;

	@KafkaListener(
			topics = TOPIC_BOOKING_STATUS_UPDATED,
			groupId = "restaurant-service-group")
	public void handleBookingStatusUpdated(String eventString) {
		try {
			BookingEvent.BookingStatusUpdatedEvent event =
					objectMapper.readValue(eventString, BookingEvent.BookingStatusUpdatedEvent.class);

			// Handle CANCELLATIONS only
			if (event.newStatus() == BookingStatus.CANCELED) {
				orderService.cancelOrderByBookingId(event.bookingId());
				log.info("Order cancelled due to booking cancellation: {}", event.bookingId());
			}

			// DO NOT handle PAID here. Let the Payment Consumer do it.
		} catch (Exception e) {
			log.error("Booking Status Consumer Error: {}", e.getMessage());
		}
	}
}