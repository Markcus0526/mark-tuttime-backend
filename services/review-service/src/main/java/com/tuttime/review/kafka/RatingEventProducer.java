package com.tuttime.review.kafka;

import com.tuttime.common.event.EventProducer;
import com.tuttime.common.event.ReviewEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingEventProducer {

	EventProducer eventProducer;

	public void sendReviewApproved(ReviewEvent.ReviewApprovedEvent event) {

		// Use restaurantId as the message key to ensure ordered processing in Kafka partitions
		eventProducer.sendReviewApprovedEvent(event);
	}
}