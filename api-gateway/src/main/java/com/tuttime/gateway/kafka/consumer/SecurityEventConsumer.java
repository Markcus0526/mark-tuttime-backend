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
@Slf4j
@RequiredArgsConstructor
public class SecurityEventConsumer {

	private final StringRedisTemplate redisTemplate;
	private static final String USER_BLOCK_PREFIX = "blocklist:user:";
	private static final String JTI_BLACKLIST_PREFIX = "blacklist:jti:";
	private final ObjectMapper objectMapper;

	@KafkaListener(
			topics = TOPIC_USER_SECURITY_UPDATED,
			groupId = "gateway-group")
	public void handleSecurityEvent(String eventString) {

		UserEvent.SecurityEvent event = null;
		try {
			log.debug("Parsing JSON event (length: {} bytes)", eventString.length());

			// Pass the Record class directly to the readValue method
			event = objectMapper.readValue(eventString, UserEvent.SecurityEvent.class);

		} catch (JsonProcessingException e) { // Better to catch specific Jackson exceptions
			log.error("Failed to parse JSON into SecurityEvent: {}", e.getMessage());
			// Optionally: throw a custom exception or handle the null 'event' below
		}

		switch (event.type()) {
			case "USER_BLOCKED":
				// Mark user as blocked in Redis indefinitely (or for a set duration)
				redisTemplate.opsForValue().set(
						USER_BLOCK_PREFIX + event.userId(),
						"true"
				);
				break;

			case "USER_UNBLOCKED":
				// Remove from blocklist
				redisTemplate.delete(USER_BLOCK_PREFIX + event.userId());
				break;

			case "TOKEN_REVOKED":
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
			case "USER_DELETED":
				// Add to Redis with no expiration (permanent block)
				redisTemplate.opsForValue().set(USER_BLOCK_PREFIX + event.userId(), "deleted");
				log.warn("Gateway updated: User {} is now permanently blocked (Deleted)", event.userId());
				break;

			default:
				log.warn("Unknown event type: {}", event.type());
		}
	}
}
