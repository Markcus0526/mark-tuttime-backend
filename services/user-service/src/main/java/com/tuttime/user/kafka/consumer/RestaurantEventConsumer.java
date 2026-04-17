package com.tuttime.user.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.event.RestaurantEvent;
import com.tuttime.user.entity.UserRole;
import com.tuttime.user.repository.RoleRepository;
import com.tuttime.user.repository.UserRepository;
import com.tuttime.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

	@KafkaListener(
			topics = TOPIC_RESTAURANT_STAFF_UPDATED,
			groupId = "user-service-group")
	@Transactional // Ensures the entire update/delete is atomic
	public void handleStaffUpdated(String eventString) {
		RestaurantEvent.StaffUpdatedEvent event = null;
		try {
			// 1. Handle Null Event
			if (!StringUtils.hasText(eventString)) {
				log.error("Received null event");
				return;
			}

			event = objectMapper.readValue(eventString, RestaurantEvent.StaffUpdatedEvent.class);
			RestaurantEvent.StaffUpdatedEvent finalEvent = event;

			userRepository.findByIdAndDeletedFalse(finalEvent.userId()).ifPresentOrElse(user -> {

				// 1. Determine which role name to use
				String targetRoleName = finalEvent.active() ? finalEvent.roleName() : "USER";

				// 2. Fetch the target role from DB
				roleRepository.findByName(targetRoleName).ifPresentOrElse(newRole -> {

					// 1. Find the current role record
					Optional<UserRole> existingRoleOpt = userRoleRepository.findByUserId(user.getId());

					if (existingRoleOpt.isPresent()) {
						UserRole existing = existingRoleOpt.get();

						// 2. Optimization: If they already have this role, do nothing
						if (existing.getRole().getId().equals(newRole.getId())) {
							log.info("User {} already has role {}, no update needed.", user.getId(), targetRoleName);
							return;
						}

						// 3. Delete the old record (crucial for composite keys)
						userRoleRepository.delete(existing);
						// Force Hibernate to sync with DB immediately to avoid "duplicate key" errors on next step
						userRoleRepository.flush();
					}

					// 4. Create and save the new record with the new Role ID
					UserRole newUserRole = UserRole.builder()
							.user(user)
							.role(newRole)
							.build();

					userRoleRepository.save(newUserRole);
					log.info("Successfully changed User {} role to {}", user.getId(), targetRoleName);

				}, () -> log.error("CRITICAL: Role '{}' not found in database", targetRoleName));

			}, () -> log.warn("User {} not found, skipping staff update", finalEvent.userId()));

		} catch (Exception e) {
			// 5. Final safety net to prevent infinite retries of bad messages
			log.error("Fatal error in Kafka Consumer: {}", e.getMessage(), e);
		}
	}
}