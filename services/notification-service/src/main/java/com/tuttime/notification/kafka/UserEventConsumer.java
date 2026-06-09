package com.tuttime.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.enums.UserStatusEvent;
import com.tuttime.common.event.kafka.event.UserEvent;
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
import java.util.concurrent.TimeUnit;

import static com.tuttime.common.event.kafka.TopicConstants.*;
import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_USER_SECURITY_UPDATED;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

	private final ObjectMapper objectMapper;
	private final NotificationOrchestrator notificationOrchestrator;

	@KafkaListener(
			topics = {TOPIC_USER_REGISTERED, TOPIC_USER_VERIFICATION_REQUESTED},
			groupId = "${spring.kafka.consumer.group-id}")
	public void handleVerificationEvents(String eventString, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			UserEvent.VerificationEvent event = objectMapper.readValue(eventString, UserEvent.VerificationEvent.class);

			if (event.templateKey() == null) {
				log.error("<<< KAFKA[{}] >>> Consumer:[notification-service], Error: Received event with null templateKey {}",
						topic, event);
				return;
			}
			log.info("<<< KAFKA[{}] >>> Consumer:[notification-service], {}", topic, event);

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
			throw new RuntimeException("notification-service failed to process " + topic, e);
		}
	}

	@KafkaListener(
			topics = TOPIC_USER_SECURITY_UPDATED,
			groupId = "${spring.kafka.consumer.group-id}")
	public void handleSecurityEvent(String eventString, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			UserEvent.SecurityEvent event = objectMapper.readValue(eventString, UserEvent.SecurityEvent.class);

			if (event.templateKey() == null) {
				log.error("<<< KAFKA[{}] >>> Consumer:[notification-service], Error: Received event with null templateKey {}",
						topic, event);
				return;
			}
			log.info("<<< KAFKA[{}] >>> Consumer:[notification-service], {}", topic, event);

			// 2. Prepare Params (Verification code, user name, etc.)
			Map<String, Object> params = new HashMap<>();
			if (event.parameters() != null) {
				params.put("email", event.parameters().get("email"));
				params.put("phoneNumber", event.parameters().get("phoneNumber"));
			}

			// 3. Trigger Orchestrator
			notificationOrchestrator.handleNotification(
					event.eventId(),
					event.userId(),
					event.templateKey(),
					params);

		} catch (Exception e) {
			log.error("<<< KAFKA[{}] >>> Consumer:[notification-service], Error: {}", TOPIC_USER_SECURITY_UPDATED, e.getMessage());
			throw new RuntimeException("notification-service failed to process " + TOPIC_USER_SECURITY_UPDATED, e);
		}
	}
}
