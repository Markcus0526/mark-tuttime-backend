package com.tuttime.restaurant.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.event.UserEvent;
import com.tuttime.restaurant.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.tuttime.common.event.EventProducer.TOPIC_USER_SECURITY_UPDATED;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

	private final ObjectMapper objectMapper;
	private final StaffRepository staffRepository;

	@KafkaListener(
			topics = TOPIC_USER_SECURITY_UPDATED,
			groupId = "restaurant-service-group")
	@Transactional
	public void handleSecurityEvent(String eventString) {
		UserEvent.SecurityEvent event = null;

		try {
			if (org.apache.commons.lang3.StringUtils.isEmpty(eventString)) {
				log.error("Received null event");
				return;
			}

			event = objectMapper.readValue(eventString, UserEvent.SecurityEvent.class);
			UserEvent.SecurityEvent finalEvent = event;

			staffRepository.findByUserId(finalEvent.userId()).ifPresentOrElse(user -> {
				switch (finalEvent.type()) {
					case "USER_BLOCKED", "USER_UNBLOCKED" -> {
						boolean shouldBeActive = finalEvent.type().equals("USER_UNBLOCKED");
						staffRepository.findByUserId(finalEvent.userId())
								.ifPresent(staff -> staff.setActive(shouldBeActive));
					}

					case "USER_DELETED" -> staffRepository.deleteByUserId(finalEvent.userId());

					default -> log.warn("Unknown event type: {}", finalEvent.type());
				}

			}, () -> log.warn("User {} not found or deleted, skipping update", finalEvent.userId()));

		} catch (Exception e) {
			log.error("Fatal error in Kafka Consumer: {}", e.getMessage(), e);
		}
	}
}