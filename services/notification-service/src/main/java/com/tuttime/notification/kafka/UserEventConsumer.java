package com.tuttime.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.event.kafka.UserEvent;
import com.tuttime.notification.service.NotificationOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_USER_REGISTERED;
import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_USER_VERIFICATION_REQUESTED;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

	private final ObjectMapper objectMapper;
	private final NotificationOrchestrator notificationOrchestrator;

	@KafkaListener(
			topics = {TOPIC_USER_REGISTERED, TOPIC_USER_VERIFICATION_REQUESTED},
			groupId = "${spring.kafka.consumer.group-id}")
	public void handleUserEvents(String eventString, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			UserEvent.VerificationEvent event = objectMapper.readValue(eventString, UserEvent.VerificationEvent.class);

			if (event.templateKey() == null) {
				log.error("<<< KAFKA[{}] >>> Consumer:[notification-service], Error: Received event with null templateKey {}",
						topic, event);
				return;
			}

			// 2. Prepare Params (Verification code, user name, etc.)
			Map<String, Object> params = new HashMap<>();
			params.put("email", event.email());
			if (event.verificationCode() != null) {
				params.put("code", event.verificationCode());
			}

			// 3. Trigger Orchestrator
			notificationOrchestrator.handleNotification(
					event.eventId(),
					event.userId(),
					event.templateKey(),
					params);

		} catch (Exception e) {
			log.error("<<< KAFKA[{}] >>> Consumer:[notification-service], Error: {}", topic, e.getMessage());
		}
	}
}
