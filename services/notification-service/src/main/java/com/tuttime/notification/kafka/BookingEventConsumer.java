package com.tuttime.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.enums.NotificationTemplateKey;
import com.tuttime.common.event.kafka.BookingEvent;
import com.tuttime.notification.service.NotificationOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_BOOKING_STATUS_UPDATED;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingEventConsumer {

	private final ObjectMapper objectMapper;
	private final NotificationOrchestrator notificationOrchestrator;

	/**
	 * Perfect Consumer Implementation for Spring Boot 3.3.13
	 * Processes 'BOOKING_CREATED' events for heatmaps and reporting.
	 */
	@KafkaListener(
			topics = TOPIC_BOOKING_STATUS_UPDATED,
			groupId = "${spring.kafka.consumer.group-id}")
	public void handleBookingStatusUpdated(String eventString) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			BookingEvent.BookingStatusUpdatedEvent event =
					objectMapper.readValue(eventString, BookingEvent.BookingStatusUpdatedEvent.class);

			// 1. Map Status to Template Key using modern switch
			NotificationTemplateKey key = switch (event.newStatus()) {
				case CREATED -> NotificationTemplateKey.BOOKING_CREATED;
				case CANCELED -> NotificationTemplateKey.BOOKING_CANCELED;
				case COMPLETED -> NotificationTemplateKey.BOOKING_COMPLETED;
				default -> null;
			};

			if (key != null) {
				log.info("Kafka: Processing {} notification for booking {}", key, event.bookingId());

				// 2. Prepare Template Parameters
				Map<String, Object> params = Map.of(
						"bookingId", event.bookingId(),
						"status", event.newStatus().toString(),
						"startTime", event.bookingTime().toString()
						// Add any other dynamic fields needed for the email/push
				);

				// 3. Trigger the Orchestrator
				// userId comes from the event; orchestrator handles gRPC lookup
				notificationOrchestrator.handleNotification(
						event.eventId(),
						event.userId(),
						key,
						params);
			}
		} catch (Exception e) {
			log.error("<<< KAFKA[{}] >>> Consumer:[notification-service], Error: {}", TOPIC_BOOKING_STATUS_UPDATED, e.getMessage());
		}
	}
}
