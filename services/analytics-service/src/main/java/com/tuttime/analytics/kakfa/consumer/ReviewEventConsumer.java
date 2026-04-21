package com.tuttime.analytics.kakfa.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.analytics.service.AnalyticsService;
import com.tuttime.common.event.BookingEvent;
import com.tuttime.common.event.ReviewEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import static com.tuttime.common.event.EventProducer.TOPIC_REVIEW_APPROVED;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewEventConsumer {

	private final ObjectMapper objectMapper;
	private final AnalyticsService analyticsService;

	/**
	 * Perfect Consumer for Spring Boot 3.3.13
	 * Tracks 'REVIEW_APPROVED' events for restaurant performance history.
	 */
	@KafkaListener(
			topics = TOPIC_REVIEW_APPROVED,
			groupId = "analytics-service-group",
			containerFactory = "kafkaListenerContainerFactory"
	)
	public void handleReviewApproved(String eventString) {
		ReviewEvent.ReviewApprovedEvent event = null;

		try {
			if (org.apache.commons.lang3.StringUtils.isEmpty(eventString)) {
				log.error("Received null event");
				return;
			}

			event = objectMapper.readValue(eventString, ReviewEvent.ReviewApprovedEvent.class);
			ReviewEvent.ReviewApprovedEvent finalEvent = event;

			log.info("Analytics received Review Approved: {} for Restaurant: {}",
					finalEvent.reviewId(), finalEvent.branchId());

			// Mapping to generic Analytics log
			analyticsService.logEvent(
					"REVIEW_SERVICE",    // Source service
					"REVIEW_APPROVED",   // Event type (for filtering in reports)
					finalEvent.branchId(),// Linked entity ID
					finalEvent                // JSONB Payload (Rating, Comment, etc.)
			);
		} catch (Exception e) {
			log.error("Fatal error in Kafka Consumer: {}", e.getMessage(), e);
		}
	}
}
