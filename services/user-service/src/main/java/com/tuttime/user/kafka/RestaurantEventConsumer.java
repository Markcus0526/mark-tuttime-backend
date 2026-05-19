package com.tuttime.user.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.event.kafka.event.RestaurantEvent;
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

import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_RESTAURANT_STAFF_UPDATED;


@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantEventConsumer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = TOPIC_RESTAURANT_STAFF_UPDATED,
            groupId = "${spring.kafka.consumer.group-id}")
    @Transactional // Ensures the entire update/delete is atomic
    public void handleStaffUpdated(String eventString) {
        try {
            if (!StringUtils.hasText(eventString)) return;

            RestaurantEvent.StaffUpdatedEvent event =
                    objectMapper.readValue(eventString, RestaurantEvent.StaffUpdatedEvent.class);

            userRepository.findByIdAndDeletedFalse(event.userId()).ifPresentOrElse(user -> {

                // 1. Determine which role name to use
                String targetRoleName = event.active() ? event.roleName() : "USER";

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

            }, () -> log.warn("User {} not found, skipping staff update", event.userId()));

        } catch (Exception e) {
            log.error("<<< KAFKA[{}] >>> Consumer:[user-service], Error: {}",
                    TOPIC_RESTAURANT_STAFF_UPDATED, e.getMessage());
        }
    }
}