package com.tuttime.gateway.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.enums.UserStatusEvent;
import com.tuttime.common.event.kafka.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_USER_SECURITY_UPDATED;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

	private final StringRedisTemplate redisTemplate;
	private static final String USER_BLOCK_PREFIX = "blocklist:user:";
	private static final String JTI_BLACKLIST_PREFIX = "blacklist:jti:";
	private final ObjectMapper objectMapper;

	@KafkaListener(
			topics = TOPIC_USER_SECURITY_UPDATED,
			groupId = "${spring.kafka.consumer.group-id}")
	public void handleSecurityEvent(String eventString) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			UserEvent.SecurityEvent event = objectMapper.readValue(eventString, UserEvent.SecurityEvent.class);

			switch (event.type()) {
				case UserStatusEvent.USER_BLOCKED:
					// Mark user as blocked in Redis indefinitely (or for a set duration)
					redisTemplate.opsForValue().set(
							USER_BLOCK_PREFIX + event.userId(),
							"true"
					);
					break;

				case UserStatusEvent.USER_UNBLOCKED:
					// Remove from blocklist
					redisTemplate.delete(USER_BLOCK_PREFIX + event.userId());
					break;

				case UserStatusEvent.TOKEN_REVOKED:
					// Blacklist specific JTI until it expires
					if (event.jti() != null) {
						redisTemplate.opsForValue().set(
								JTI_BLACKLIST_PREFIX + event.jti(),
								"revoked",
								event.ttlMillis(),
								TimeUnit.MILLISECONDS
						);
					}
					break;

				// Inside your SecurityEventConsumer (Gateway)
				case UserStatusEvent.USER_DELETED:
					// Add to Redis with no expiration (permanent block)
					redisTemplate.opsForValue().set(USER_BLOCK_PREFIX + event.userId(), "deleted");
					log.warn("Gateway updated: User {} is now permanently blocked (Deleted)", event.userId());
					break;

				default:
					log.warn("Unknown event type: {}", event.type());
			}
		} catch (Exception e) {
			log.error("<<< KAFKA[{}] >>> Consumer:[analytics-service], Error: {}", TOPIC_USER_SECURITY_UPDATED, e.getMessage());
		}
	}
}
