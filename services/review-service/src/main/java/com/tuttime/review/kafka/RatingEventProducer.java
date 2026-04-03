package com.tuttime.review.kafka;

import com.tuttime.common.event.EventProducer;
import com.tuttime.common.event.ReviewEvent;
import com.tuttime.review.entity.RatingAggregate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingEventProducer {

	EventProducer eventProducer;

	public void sendRatingUpdate(RatingAggregate agg) {
		log.info("Sending rating update for restaurant: {}", agg.getRestaurantId());

		// Use restaurantId as the message key to ensure ordered processing in Kafka partitions
		eventProducer.publishReviewRatingUpdatedEvent(new ReviewEvent.RatingUpdateEvent(
				agg.getRestaurantId(),
				agg.getAverageRating(),
				agg.getTotalReviews()));
	}
}