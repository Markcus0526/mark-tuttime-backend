package com.tuttime.restaurant.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.enums.UserStatusEvent;
import com.tuttime.common.event.kafka.UserEvent;
import com.tuttime.restaurant.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_USER_SECURITY_UPDATED;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

	private final ObjectMapper objectMapper;
	private final StaffRepository staffRepository;

	@KafkaListener(
			topics = TOPIC_USER_SECURITY_UPDATED,
			groupId = "${spring.kafka.consumer.group-id}")
	@Transactional
	public void handleSecurityEvent(String eventString) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			UserEvent.SecurityEvent event = objectMapper.readValue(eventString, UserEvent.SecurityEvent.class);

			staffRepository.findByUserId(event.userId()).ifPresentOrElse(user -> {
				switch (event.type()) {
					case UserStatusEvent.USER_BLOCKED, UserStatusEvent.USER_UNBLOCKED -> {
						boolean shouldBeActive = event.type().equals(UserStatusEvent.USER_UNBLOCKED);
						staffRepository.findByUserId(event.userId())
								.ifPresent(staff -> staff.setActive(shouldBeActive));
					}

					case UserStatusEvent.USER_DELETED -> staffRepository.deleteByUserId(event.userId());

					default -> log.warn("Unknown event type: {}", event.type());
				}

			}, () -> log.warn("User {} not found or deleted, skipping update", event.userId()));

		} catch (Exception e) {
			log.error("<<< KAFKA[{}] >>> Consumer:[restaurant-service], Error: {}", TOPIC_USER_SECURITY_UPDATED, e.getMessage());
		}
	}
}