package com.tuttime.gateway.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.enums.UserStatusEvent;
import com.tuttime.common.event.kafka.event.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate; // 🛑 Swapped to Reactive
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.time.Duration;

import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_USER_SECURITY_UPDATED;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    // 🛑 STABILITY FIX 1: Utilize Non-Blocking Reactive templates to protect Netty event loops
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    private static final String USER_BLOCK_PREFIX = "blocklist:user:";
    private static final String JTI_BLACKLIST_PREFIX = "blacklist:jti:";
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = TOPIC_USER_SECURITY_UPDATED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory") // Wired to our Step 4 secure DLQ factory
    public void handleSecurityEvent(String eventString) {
        try {
            if (!StringUtils.hasText(eventString)) return;

            UserEvent.SecurityEvent event = objectMapper.readValue(eventString, UserEvent.SecurityEvent.class);

            switch (event.type()) {
                case UserStatusEvent.USER_BLOCKED:
                    // Async subscription execution protects pipeline throughput speeds
                    reactiveRedisTemplate.opsForValue()
                            .set(USER_BLOCK_PREFIX + event.userId(), "true")
                            .subscribe(success -> log.info("Gateway: Marked User {} as BLOCKED in cache.", event.userId()));
                    break;

                case UserStatusEvent.USER_UNBLOCKED:
                    reactiveRedisTemplate.delete(USER_BLOCK_PREFIX + event.userId())
                            .subscribe(success -> log.info("Gateway: Removed User {} from blocklist cache.", event.userId()));
                    break;

                case UserStatusEvent.TOKEN_REVOKED:
                    if (event.jti() != null) {
                        // Enforce expiration rules natively utilizing clean Duration abstractions
                        reactiveRedisTemplate.opsForValue()
                                .set(JTI_BLACKLIST_PREFIX + event.jti(), "revoked", Duration.ofMillis(event.ttlMillis()))
                                .subscribe(success -> log.info("Gateway: Revoked JTI token key: {}", event.jti()));
                    }
                    break;

                case UserStatusEvent.USER_DELETED:
                    reactiveRedisTemplate.opsForValue()
                            .set(USER_BLOCK_PREFIX + event.userId(), "deleted")
                            .subscribe(success -> log.warn("Gateway updated: User {} is permanently blocked (Deleted)", event.userId()));
                    break;

                default:
                    log.warn("Unknown event type parameter: {}", event.type());
            }
        } catch (Exception e) {
            log.error("<<< KAFKA[{}] >>> Consumer:[api-gateway] Processing failure occurred: {}", TOPIC_USER_SECURITY_UPDATED, e.getMessage());

            // 🛑 RELIABILITY FIX 2: Re-throw the exception!
            // This triggers Step 4 cluster retries + automated fallback routing to your backup queue.
            throw new RuntimeException("Failed to synchronize gateway security blocklist state", e);
        }
    }
}
