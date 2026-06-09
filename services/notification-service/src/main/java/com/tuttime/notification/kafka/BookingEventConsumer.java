package com.tuttime.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.enums.BookingStatus;
import com.tuttime.common.enums.NotificationTemplateKey;
import com.tuttime.common.event.kafka.event.BookingEvent;
import com.tuttime.common.event.proto.GetBranchManagersRequest;
import com.tuttime.common.event.proto.GetBranchManagersResponse;
import com.tuttime.notification.grpc.client.RestaurantGrpcClient;
import com.tuttime.notification.service.NotificationOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;

import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_BOOKING_STATUS_UPDATED;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

	private final ObjectMapper objectMapper;
	private final NotificationOrchestrator notificationOrchestrator;
	private final RestaurantGrpcClient restaurantGrpcClient;

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
			NotificationTemplateKey customerKey = switch (event.newStatus()) {
				case CREATED -> NotificationTemplateKey.BOOKING_CREATED;
				case CANCELED -> NotificationTemplateKey.BOOKING_CANCELED;
				case COMPLETED -> NotificationTemplateKey.BOOKING_COMPLETED;
				default -> null;
			};

			if (customerKey != null) {
				log.info("Kafka: Processing {} notification for booking {}", customerKey, event.bookingId());

				// 1. Notify the Customer (userId is already in the event)
				Map<String, Object> params = Map.of(
						"bookingId", event.bookingId(),
						"status", event.newStatus().toString(),
						"startTime", event.bookingTime().toString()
						// Add any other dynamic fields needed for the email/push
				);
				notificationOrchestrator.handleNotification(
						event.eventId(),
						event.userId(),
						customerKey,
						params);
			}

			if (shouldNotifyManagers(event.newStatus())) {
				NotificationTemplateKey managerKey = (event.newStatus() == BookingStatus.CREATED)
						? NotificationTemplateKey.MANAGER_BOOKING_ALERT
						: NotificationTemplateKey.MANAGER_BOOKING_CANCEL;

				Thread.ofVirtual().start(() -> {
					try {
						// FIX: Pass event.branchId(), not bookingTime
						GetBranchManagersResponse response = restaurantGrpcClient.getBranchManagers(
								GetBranchManagersRequest.newBuilder()
										.setBranchId(event.branchId().toString())
										.build());

						response.getManagerIdsList().stream()
								.map(UUID::fromString)
								.forEach(managerId -> notificationOrchestrator.handleNotification(
										event.eventId(),
										managerId,
										managerKey,
										buildParams(event)
								));
					} catch (Exception e) {
						log.error("Manager fan-out failed for branch {}: {}", event.branchId(), e.getMessage());
					}
				});
			}
		} catch (Exception e) {
			log.error("<<< KAFKA[{}] >>> Consumer:[notification-service], Error: {}", TOPIC_BOOKING_STATUS_UPDATED, e.getMessage());
			throw new RuntimeException("notification-service failed to process " + TOPIC_BOOKING_STATUS_UPDATED, e);
		}
	}

	private boolean shouldNotifyManagers(BookingStatus status) {
		return status == BookingStatus.CREATED || status == BookingStatus.CANCELED;
	}

	private Map<String, Object> buildParams(BookingEvent.BookingStatusUpdatedEvent event) {
		return Map.of(
				"bookingId", event.bookingId(),
				"time", event.bookingTime().toString(),
				"status", event.newStatus().name()
		);
	}
}
