package com.tuttime.analytics.kakfa.consumer;

import com.tuttime.analytics.service.AnalyticsService;
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
	public void handleReviewApproved(ReviewEvent.ReviewApprovedEvent event, Acknowledgment ack) {
		log.info("Analytics received Review Approved: {} for Restaurant: {}",
				event.reviewId(), event.restaurantId());

		try {
			// Mapping to generic Analytics log
			analyticsService.logEvent(
					"REVIEW_SERVICE",    // Source service
					"REVIEW_APPROVED",   // Event type (for filtering in reports)
					event.restaurantId(),// Linked entity ID
					event                // JSONB Payload (Rating, Comment, etc.)
			);

			ack.acknowledge();

		} catch (Exception e) {
			log.error("Failed to log review analytics {}: {}",
					event.reviewId(), e.getMessage());
		}
	}
}
