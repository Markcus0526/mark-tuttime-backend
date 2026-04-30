package com.tuttime.restaurant.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.event.kafka.event.ReviewEvent;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_REVIEW_APPROVED;


@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventConsumer {

	private final BranchRepository branchRepository;
	private final RestaurantRepository restaurantRepository;
	private final ObjectMapper objectMapper;

	@KafkaListener(
			topics = TOPIC_REVIEW_APPROVED,
			groupId = "${spring.kafka.consumer.group-id}")
	@Transactional
	public void handleRatingUpdate(String eventString) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			ReviewEvent.ReviewApprovedEvent event = objectMapper.readValue(eventString, ReviewEvent.ReviewApprovedEvent.class);
			if (event.eventId() == null || event.branchId() == null) {
				log.debug("Received empty Review update for branch {}", event.branchId());
				return;
			}

			// 1. Update Branch Rating
			Branch branch = branchRepository.findById(event.branchId())
					.orElseThrow(() -> new InvalidRequestException("Branch not found"));

			// Note: In a real production app, the event should carry the
			// NEW calculated average from the Review Service to ensure 100% sync.
			branch.setRating(BigDecimal.valueOf(event.rating() != null ? event.rating() : 0.0));
			branchRepository.save(branch);

			// 2. Update Parent Restaurant Rating (Average of all its branches)
			updateParentRestaurantRating(branch.getRestaurant().getId());
		} catch (Exception e) {
			log.error("<<< KAFKA[{}] >>> Consumer:[restaurant-service], Error: {}", TOPIC_REVIEW_APPROVED, e.getMessage());
		}
	}

	private void updateParentRestaurantRating(UUID restaurantId) {
		Double restaurantAvg = branchRepository.getAverageRatingForRestaurant(restaurantId);

		// 3. Update the Restaurant table
		Restaurant restaurant = restaurantRepository.findById(restaurantId)
				.orElseThrow(() -> new InvalidRequestException("Restaurant not found"));

		restaurant.setRating(BigDecimal.valueOf(restaurantAvg != null ? restaurantAvg : 0.0));
		restaurantRepository.save(restaurant);

		log.info("Synced ratings: Restaurant {} is now {}",
				restaurantId, restaurant.getRating());
	}
}
