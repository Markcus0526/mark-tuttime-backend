package com.tuttime.analytics.kakfa.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.analytics.service.AnalyticsService;
import com.tuttime.common.event.BookingEvent;
import com.tuttime.common.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import static com.tuttime.common.event.EventProducer.TOPIC_BOOKING_STATUS_UPDATED;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingEventConsumer {

	private final ObjectMapper objectMapper;
	private final AnalyticsService analyticsService;

	/**
	 * Perfect Consumer Implementation for Spring Boot 3.3.13
	 * Processes 'BOOKING_CREATED' events for heatmaps and reporting.
	 */
	@KafkaListener(
			topics = TOPIC_BOOKING_STATUS_UPDATED,
			groupId = "analytics-service-group")
	public void handleBookingStatusUpdated(String eventString) {
		BookingEvent.BookingStatusUpdatedEvent event = null;

		try {
			if (org.apache.commons.lang3.StringUtils.isEmpty(eventString)) {
				log.error("Received null event");
				return;
			}

			event = objectMapper.readValue(eventString, BookingEvent.BookingStatusUpdatedEvent.class);
			BookingEvent.BookingStatusUpdatedEvent finalEvent = event;

			log.info("Analytics received Booking Event: {} for Restaurant: {}",
					finalEvent.bookingId(), finalEvent.bookingId());

			// Mapping the specific event to the generic Analytics log
			analyticsService.logEvent(
					"BOOKING_SERVICE",   // service name
					finalEvent.newStatus().name(),  // event type (used for Heatmap/Top 10 queries)
					finalEvent.bookingId(),// entityId (linked to the restaurant)
					finalEvent                // full payload (stored as JSONB)
			);
		} catch (Exception e) {
			log.error("Fatal error in Kafka Consumer: {}", e.getMessage(), e);
		}
	}
}
