package com.tuttime.gateway.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.event.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.tuttime.common.event.EventProducer.TOPIC_USER_SECURITY_UPDATED;

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
			groupId = "gateway-group")
	public void consumeSecurityEvent(String eventString) {
		UserEvent.SecurityEvent event = null;

		try {
			if (org.apache.commons.lang3.StringUtils.isEmpty(eventString)) {
				log.error("Received null event");
				return;
			}

			event = objectMapper.readValue(eventString, UserEvent.SecurityEvent.class);
			UserEvent.SecurityEvent finalEvent = event;

			switch (finalEvent.type()) {
				case "USER_BLOCKED":
					// Mark user as blocked in Redis indefinitely (or for a set duration)
					redisTemplate.opsForValue().set(
							USER_BLOCK_PREFIX + finalEvent.userId(),
							"true"
					);
					break;

				case "USER_UNBLOCKED":
					// Remove from blocklist
					redisTemplate.delete(USER_BLOCK_PREFIX + finalEvent.userId());
					break;

				case "TOKEN_REVOKED":
					// Blacklist specific JTI until it expires
					if (finalEvent.jti() != null) {
						redisTemplate.opsForValue().set(
								JTI_BLACKLIST_PREFIX + finalEvent.jti(),
								"revoked",
								finalEvent.ttlMillis(),
								TimeUnit.MILLISECONDS
						);
					}
					break;

				// Inside your SecurityEventConsumer (Gateway)
				case "USER_DELETED":
					// Add to Redis with no expiration (permanent block)
					redisTemplate.opsForValue().set(USER_BLOCK_PREFIX + finalEvent.userId(), "deleted");
					log.warn("Gateway updated: User {} is now permanently blocked (Deleted)", finalEvent.userId());
					break;

				default:
					log.warn("Unknown event type: {}", finalEvent.type());
			}
		} catch (Exception e) {
			log.error("Fatal error in Kafka Consumer: {}", e.getMessage(), e);
		}
	}
}
