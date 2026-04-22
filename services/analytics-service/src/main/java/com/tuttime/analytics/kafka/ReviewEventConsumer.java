package com.tuttime.analytics.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.analytics.service.AnalyticsService;
import com.tuttime.common.event.kafka.ReviewEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.tuttime.analytics.constants.AnalyticsConstants.EVENT_REVIEW_APPROVED;
import static com.tuttime.analytics.constants.AnalyticsConstants.SOURCE_REVIEW_SERVICE;
import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_REVIEW_APPROVED;

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
			groupId = "${spring.kafka.consumer.group-id}")
	public void handleReviewApproved(String eventString) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			ReviewEvent.ReviewApprovedEvent event = objectMapper.readValue(eventString, ReviewEvent.ReviewApprovedEvent.class);

			log.info("Analytics received Review Approved: {} for Restaurant: {}",
					event.reviewId(), event.branchId());

			// Mapping to generic Analytics log
			analyticsService.processEvent(
					SOURCE_REVIEW_SERVICE,    // Source service
					EVENT_REVIEW_APPROVED,   // Event type (for filtering in reports)
					event.branchId(),// Linked entity ID
					event                // JSONB Payload (Rating, Comment, etc.)
			);
		} catch (Exception e) {
			log.error("<<< KAFKA[{}] >>> Consumer:[analytics-service], Error: {}", TOPIC_REVIEW_APPROVED, e.getMessage());
		}
	}
}
