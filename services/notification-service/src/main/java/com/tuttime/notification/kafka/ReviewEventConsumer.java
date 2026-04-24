package com.tuttime.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.enums.NotificationTemplateKey;
import com.tuttime.common.event.kafka.ReviewEvent;
import com.tuttime.notification.service.NotificationOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_REVIEW_APPROVED;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewEventConsumer {

	private final ObjectMapper objectMapper;
	private final NotificationOrchestrator notificationOrchestrator;

	/**
	 * Perfect Consumer for Spring Boot 3.3.13
	 * Tracks 'REVIEW_APPROVED' events for restaurant performance history.
	 */
	@KafkaListener(
			topics = TOPIC_REVIEW_APPROVED,
			groupId = "${spring.kafka.consumer.group-id}")
	public void handleReviewApproved(String eventString) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			ReviewEvent.ReviewApprovedEvent event = objectMapper.readValue(eventString, ReviewEvent.ReviewApprovedEvent.class);

			log.info("Kafka: Review approved for Branch: {}. Notifying user: {}",
					event.branchId(), event.userId());

			// 1. Prepare dynamic parameters for the notification template
			// These can be used in your HTML/SMS via {{rating}}, {{reviewId}}, etc.
			Map<String, Object> params = Map.of(
					"reviewId", event.reviewId(),
					"rating", event.rating()
//					"comment", event.comment() != null ? event.comment() : "",
//					"branchName", event.branchName() // Assuming this is in the event
			);

			// 2. Trigger Orchestrator
			// We use 'REVIEW_APPROVED' as the template_key in our database
			notificationOrchestrator.handleNotification(
					event.eventId(),
					event.userId(),  // The ID of the person being notified (e.g., the Tutor)
					NotificationTemplateKey.REVIEW_APPROVED,      // The Template Key
					params);
		} catch (Exception e) {
			log.error("<<< KAFKA[{}] >>> Consumer:[notification-service], Error: {}", TOPIC_REVIEW_APPROVED, e.getMessage());
		}
	}
}
