package com.tuttime.user.kafka;

import com.tuttime.common.kafka.KafkaMetrics;
import com.tuttime.common.kafka.UserBlockEvent;
import com.tuttime.user.entity.UserBlock;
import com.tuttime.user.repository.UserBlockRepository;
import com.tuttime.user.service.UserBlockService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka Consumer for User Block Events.
 * 
 * <p>Listens to user blocking events from any service and syncs them to user-service.
 * Topic: apart-token.user-blocks
 * 
 * <p>This consumer processes blocking events published by social-service or any other service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "KAFKA_ENABLED", havingValue = "true", matchIfMissing = false)
public class UserBlockEventConsumer {

    private final UserBlockRepository userBlockRepository;
    private final UserBlockService userBlockService;
    private final ObjectMapper objectMapper;
    private final KafkaMetrics kafkaMetrics;

    @Value("${spring.kafka.consumer.group-id:user-service-group}")
    private String groupId;

    @Value("${spring.application.name:user-service}")
    private String serviceName;

    @KafkaListener(
            topics = "${spring.kafka.topics.user-blocks:apart-token.user-blocks}",
            groupId = "${spring.kafka.consumer.group-id:user-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleUserBlockEvent(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
            @Header(KafkaHeaders.OFFSET) Long offset) throws JsonProcessingException {
        Timer.Sample sample = kafkaMetrics != null ? kafkaMetrics.startProcessingTimer() : null;

        try {
            UserBlockEvent event = objectMapper.readValue(message, UserBlockEvent.class);
            log.debug("Received user block event: type={}, blocker={}, blocked={}, source={}, topic={}, partition={}, offset={}",
                    event.getEventType(), event.getBlockerUserId(), event.getBlockedUserId(), 
                    event.getSourceService(), topic, partition, offset);

            // Skip processing if event is from this service (to avoid circular processing)
            if (serviceName.equals(event.getSourceService())) {
                log.debug("Skipping event from same service: source={}", event.getSourceService());
                return;
            }

            // Process block/unblock event
            switch (event.getEventType()) {
                case USER_BLOCKED:
                    handleUserBlock(event.getBlockerUserId(), event.getBlockedUserId());
                    break;
                case USER_UNBLOCKED:
                    handleUserUnblock(event.getBlockerUserId(), event.getBlockedUserId());
                    break;
                default:
                    log.warn("Unknown user block event type: {}", event.getEventType());
            }

            // Auto-commit mode: acknowledgment is handled automatically by Kafka

            // Record success metrics
            if (kafkaMetrics != null && sample != null) {
                kafkaMetrics.recordProcessingSuccess(topic, groupId, sample);
            }
        } catch (Exception e) {
            log.error("Error processing user block event from topic: {}, partition: {}, offset: {}",
                    topic, partition, offset, e);
            // Record failure metrics
            if (kafkaMetrics != null && sample != null) {
                kafkaMetrics.recordProcessingFailure(topic, groupId, sample, e.getClass().getSimpleName());
            }
            // Let exceptions propagate to error handler for retry and DLQ
            throw e;
        }
    }

    /**
     * Handles user block event from external service.
     * Syncs the block to user-service database.
     */
    private void handleUserBlock(Long blockerUserId, Long blockedUserId) {
        log.info("Syncing user block from external service: blocker={}, blocked={}", blockerUserId, blockedUserId);

        try {
            // Check if already blocked in user-service
            if (userBlockRepository.findByBlockerUserIdAndBlockedUserIdAndDeletedAtIsNull(blockerUserId, blockedUserId).isPresent()) {
                log.debug("Block already exists in user-service: blocker={}, blocked={}", blockerUserId, blockedUserId);
                return;
            }

            // Sync block to user-service (without publishing event to avoid circular dependency)
            syncBlockFromExternal(blockerUserId, blockedUserId);
            log.info("Successfully synced user block from external service: blocker={}, blocked={}", 
                    blockerUserId, blockedUserId);
        } catch (Exception e) {
            log.error("Failed to sync user block from external service: blocker={}, blocked={}", 
                    blockerUserId, blockedUserId, e);
            throw e; // Re-throw to trigger retry/DLQ
        }
    }

    /**
     * Handles user unblock event from external service.
     * Syncs the unblock to user-service database.
     */
    private void handleUserUnblock(Long blockerUserId, Long blockedUserId) {
        log.info("Syncing user unblock from external service: blocker={}, blocked={}", blockerUserId, blockedUserId);

        try {
            // Check if block exists in user-service
            if (userBlockRepository.findByBlockerUserIdAndBlockedUserIdAndDeletedAtIsNull(blockerUserId, blockedUserId).isEmpty()) {
                log.debug("Block does not exist in user-service: blocker={}, blocked={}", blockerUserId, blockedUserId);
                return;
            }

            // Sync unblock to user-service (without publishing event to avoid circular dependency)
            syncUnblockFromExternal(blockerUserId, blockedUserId);
            log.info("Successfully synced user unblock from external service: blocker={}, blocked={}", 
                    blockerUserId, blockedUserId);
        } catch (Exception e) {
            log.error("Failed to sync user unblock from external service: blocker={}, blocked={}", 
                    blockerUserId, blockedUserId, e);
            throw e; // Re-throw to trigger retry/DLQ
        }
    }

    /**
     * Syncs block from external source without publishing event (to avoid circular dependency).
     */
    private void syncBlockFromExternal(Long blockerUserId, Long blockedUserId) {
        // Check if there's a soft-deleted block (unblocked previously) - restore it
        var softDeletedBlock = userBlockRepository
                .findByBlockerUserIdAndBlockedUserId(blockerUserId, blockedUserId)
                .filter(block -> block.getDeletedAt() != null);

        if (softDeletedBlock.isPresent()) {
            // Restore the block by clearing deletedAt
            var block = softDeletedBlock.get();
            block.setDeletedAt(null);
            block.setUpdatedAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));
            userBlockRepository.save(block);
            log.info("Restored block from external sync: blocker={}, blocked={}", blockerUserId, blockedUserId);
        } else {
            // Create new block
            var block = UserBlock.builder()
                    .blockerUserId(blockerUserId)
                    .blockedUserId(blockedUserId)
                    .createdAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC))
                    .updatedAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC))
                    .deletedAt(null)
                    .build();
            userBlockRepository.save(block);
            log.info("Created block from external sync: blocker={}, blocked={}", blockerUserId, blockedUserId);
        }
    }

    /**
     * Syncs unblock from external source without publishing event (to avoid circular dependency).
     */
    private void syncUnblockFromExternal(Long blockerUserId, Long blockedUserId) {
        var block = userBlockRepository
                .findByBlockerUserIdAndBlockedUserIdAndDeletedAtIsNull(blockerUserId, blockedUserId);

        if (block.isPresent()) {
            // Soft delete the block
            var userBlock = block.get();
            userBlock.setDeletedAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));
            userBlock.setUpdatedAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));
            userBlockRepository.save(userBlock);
            log.info("Unblocked from external sync: blocker={}, blocked={}", blockerUserId, blockedUserId);
        }
    }
}

