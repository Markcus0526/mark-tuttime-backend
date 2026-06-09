package com.tuttime.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_BRANCH_BRANCH_AVAILABILITY;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantEventConsumer {

	private final ObjectMapper objectMapper;
	private final NotificationOrchestrator notificationOrchestrator;
	private final RestaurantGrpcClient restaurantGrpcClient;

	@KafkaListener(
			topics = TOPIC_BRANCH_BRANCH_AVAILABILITY, // Topic from Restaurant/Analytics Service
			groupId = "${spring.kafka.consumer.group-id}")
	public void handleLowAvailability(String eventString) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			// Define a simple event DTO for capacity alerts
			BookingEvent.BranchAvailabilityEvent event = objectMapper.readValue(eventString, BookingEvent.BranchAvailabilityEvent.class);

			log.info("Processing low availability alert for branch: {}", event.branchId());

			// Reuse the Virtual Thread fan-out pattern
			Thread.ofVirtual().start(() -> {
				try {
					GetBranchManagersResponse response = restaurantGrpcClient.getBranchManagers(
							GetBranchManagersRequest.newBuilder()
									.setBranchId(event.branchId())
									.build());

					Map<String, Object> params = Map.of(
							"branchName", event.branchName(),
							"currentOccupancy", event.occupancyPercent() + "%",
							"threshold", "90%" // Example threshold
					);

					response.getManagerIdsList().stream()
							.map(UUID::fromString)
							.forEach(managerId -> notificationOrchestrator.handleNotification(
									UUID.randomUUID(), // New event ID for this alert
									managerId,
									NotificationTemplateKey.MANAGER_LOW_AVAILABILITY,
									params
							));
				} catch (Exception e) {
					log.error("Availability fan-out failed for branch {}: {}", event.branchId(), e.getMessage());
				}
			});
		} catch (Exception e) {
			log.error("Error processing availability event: {}", e.getMessage());
			throw new RuntimeException("notification-service failed to process restaurant availability event", e);
		}
	}
}
