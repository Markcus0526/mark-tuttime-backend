package com.tuttime.user.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.event.RestaurantEvent;
import com.tuttime.common.event.UserEvent;
import com.tuttime.user.entity.UserRole;
import com.tuttime.user.repository.RoleRepository;
import com.tuttime.user.repository.UserRepository;
import com.tuttime.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.tuttime.common.event.EventProducer.TOPIC_RESTAURANT_STAFF_UPDATED;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantEventConsumer {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;
	private final ObjectMapper objectMapper;

	@Transactional // Ensures the entire update/delete is atomic
	@KafkaListener(topics = TOPIC_RESTAURANT_STAFF_UPDATED, groupId = "user-service-group")
	public void consumeStaffUpdated(String eventString) {
		RestaurantEvent.StaffUpdatedEvent event = null;
		try {

			// 1. Handle Null Event
			if (StringUtils.isEmpty(eventString)) {
				log.error("Received null event");
				return;
			}

			event = objectMapper.readValue(eventString, RestaurantEvent.StaffUpdatedEvent.class);
			// 2. Safe User Lookup
			RestaurantEvent.StaffUpdatedEvent finalEvent = event;
			userRepository.findByUserIdAndDeletedFalse(event.userId()).ifPresentOrElse(user -> {

				// Fetch current role once
				Optional<UserRole> userRoleOpt = userRoleRepository.findByUserId(user.getUserId());

				if (finalEvent.active()) {
					// 3. SAFE Role Fetching (No .get())
					roleRepository.findByName(finalEvent.roleName()).ifPresentOrElse(role -> {
						UserRole userRole = userRoleOpt.orElseGet(() -> {
							UserRole newRole = new UserRole();
							newRole.setUserId(user.getUserId());
							return newRole;
						});
						userRole.setRole(role);
						userRoleRepository.save(userRole);
						log.info("Updated User {} to Role {}", user.getUserId(), finalEvent.roleName());
					}, () -> log.error("CRITICAL: Role '{}' not found in database", finalEvent.roleName()));
				} else {
					// 4. Safe Deletion
					userRoleOpt.ifPresent(userRoleRepository::delete);
					log.info("Removed roles for User {}", user.getUserId());
				}

			}, () -> log.warn("User {} not found or deleted, skipping update", finalEvent.userId()));

		} catch (Exception e) {
			// 5. Final safety net to prevent infinite retries of bad messages
			log.error("Fatal error in Kafka Consumer: {}", e.getMessage(), e);
		}
	}
}