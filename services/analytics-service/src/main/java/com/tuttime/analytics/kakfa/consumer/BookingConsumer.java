package com.tuttime.analytics.kakfa.consumer;

import com.tuttime.analytics.service.AnalyticsService;
import com.tuttime.common.event.BookingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import static com.tuttime.common.event.EventProducer.TOPIC_BOOKING_CREATED;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingConsumer {

	private final AnalyticsService analyticsService;

	/**
	 * Perfect Consumer Implementation for Spring Boot 3.3.13
	 * Processes 'BOOKING_CREATED' events for heatmaps and reporting.
	 */
	@KafkaListener(
			topics = TOPIC_BOOKING_CREATED,
			groupId = "analytics-service-group",
			containerFactory = "kafkaListenerContainerFactory"
	)
	public void handleBookingCreated(BookingEvent.BookingCreatedEvent event, Acknowledgment ack) {
		log.info("Analytics received Booking Event: {} for Restaurant: {}",
				event.bookingId(), event.restaurantId());

		try {
			// Mapping the specific event to the generic Analytics log
			analyticsService.logEvent(
					"BOOKING_SERVICE",   // service name
					"BOOKING_CREATED",   // event type (used for Heatmap/Top 10 queries)
					event.restaurantId(),// entityId (linked to the restaurant)
					event                // full payload (stored as JSONB)
			);

			// Manual Acknowledgment ensures we don't lose data if DB is down
			ack.acknowledge();

		} catch (Exception e) {
			log.error("Failed to log analytics for booking {}: {}",
					event.bookingId(), e.getMessage());
			// In production, you'd send this to a Dead Letter Queue (DLQ)
		}
	}
}
