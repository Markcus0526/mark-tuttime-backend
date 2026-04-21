package com.tuttime.review.kafka;

import com.tuttime.common.event.EventProducer;
import com.tuttime.common.event.ReviewEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewEventProducer {

	private final EventProducer eventProducer;

	public void sendReviewApprovedEvent(ReviewEvent.ReviewApprovedEvent event) {

		// Use restaurantId as the message key to ensure ordered processing in Kafka partitions
		eventProducer.sendReviewApprovedEvent(event);
	}
}