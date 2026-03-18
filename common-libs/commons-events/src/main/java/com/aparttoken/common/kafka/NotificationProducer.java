package com.aparttoken.common.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Helper class for publishing notification events to Kafka
 * Other services can use this to send notifications
 *
 * Note: This component is only available when KafkaTemplate is configured.
 * Services should check for its availability before using it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(KafkaTemplate.class)
public class NotificationProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String NOTIFICATION_TOPIC = "apart-token.notifications";

    public void sendNotification(NotificationEvent event) {
        try {
            // Log event details before serialization for debugging
            log.debug("📤 [KAFKA_PRODUCER] Serializing notification event: userId={}, type={}, templateKey={}, templateVariables={}", 
                    event.getUserId(), event.getNotificationType(), event.getTemplateKey(), event.getTemplateVariables());
            
            String message = objectMapper.writeValueAsString(event);
            
            // Log serialized message for debugging (only if templateKey is present to avoid spam)
            if (event.getTemplateKey() != null) {
                log.info("📤 [KAFKA_PRODUCER] Serialized message (templateKey={}): {}", event.getTemplateKey(), message);
            }
            
            // Use userId as partition key to ensure ordering per user
            String partitionKey = event.getUserId() != null ? event.getUserId().toString() : null;
            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(NOTIFICATION_TOPIC, partitionKey, message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Notification event sent successfully: userId={}, type={}, templateKey={}, offset={}",
                            event.getUserId(), event.getNotificationType(), event.getTemplateKey(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send notification event: userId={}, type={}, templateKey={}",
                            event.getUserId(), event.getNotificationType(), event.getTemplateKey(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error serializing notification event: userId={}, type={}, templateKey={}",
                    event.getUserId(), event.getNotificationType(), event.getTemplateKey(), e);
        }
    }

    // Convenience methods for common notification types

    public void sendUserRegisteredNotification(Long userId, String email) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .notificationType(NotificationEvent.NotificationType.USER_REGISTERED)
                .channel(NotificationEvent.NotificationChannel.EMAIL)
                .subject("Welcome to APART TOKEN Platform")
                .message("Thank you for registering! Your account has been created successfully.")
                .priority(NotificationEvent.NotificationPriority.NORMAL)
                .referenceType("USER")
                .referenceId(String.valueOf(userId))
                .build();
        sendNotification(event);
    }

    public void sendTransactionNotification(Long userId, String type, String status, String amount, String transactionId) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .notificationType(mapTransactionType(type, status))
                .channel(NotificationEvent.NotificationChannel.IN_APP)
                .subject("Transaction " + status)
                .message(String.format("Your %s transaction of %s APART tokens has been %s", type, amount, status))
                .priority(NotificationEvent.NotificationPriority.NORMAL)
                .referenceType("TRANSACTION")
                .referenceId(transactionId)
                .build();
        sendNotification(event);
    }

    public void sendStakingNotification(Long userId, String type, String amount, String stakingId) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .notificationType(mapStakingType(type))
                .channel(NotificationEvent.NotificationChannel.IN_APP)
                .subject("Staking " + type)
                .message(String.format("Your staking of %s APART tokens has been %s", amount, type))
                .priority(NotificationEvent.NotificationPriority.NORMAL)
                .referenceType("STAKING")
                .referenceId(stakingId)
                .build();
        sendNotification(event);
    }

    public void sendRewardNotification(Long userId, String rewardType, String amount, String referenceId) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .notificationType(mapRewardType(rewardType))
                .channel(NotificationEvent.NotificationChannel.IN_APP)
                .subject("Reward Received")
                .message(String.format("You have received %s AP Credits as %s reward", amount, rewardType))
                .priority(NotificationEvent.NotificationPriority.NORMAL)
                .referenceType("REWARD")
                .referenceId(referenceId)
                .build();
        sendNotification(event);
    }

    private NotificationEvent.NotificationType mapTransactionType(String type, String status) {
        if ("FAILED".equals(status)) {
            return NotificationEvent.NotificationType.TRANSACTION_FAILED;
        }
        return switch (type) {
            case "DEPOSIT" -> NotificationEvent.NotificationType.TOKEN_DEPOSIT;
            case "WITHDRAWAL" -> NotificationEvent.NotificationType.TOKEN_WITHDRAWAL;
            case "TRANSFER" -> NotificationEvent.NotificationType.TOKEN_TRANSFER;
            default -> NotificationEvent.NotificationType.TRANSACTION_CONFIRMED;
        };
    }

    private NotificationEvent.NotificationType mapStakingType(String type) {
        return switch (type) {
            case "STARTED" -> NotificationEvent.NotificationType.STAKING_STARTED;
            case "COMPLETED" -> NotificationEvent.NotificationType.STAKING_COMPLETED;
            case "REWARD" -> NotificationEvent.NotificationType.STAKING_REWARD;
            default -> NotificationEvent.NotificationType.STAKING_STARTED;
        };
    }

    private NotificationEvent.NotificationType mapRewardType(String rewardType) {
        return switch (rewardType) {
            case "REFERRAL_BONUS" -> NotificationEvent.NotificationType.REFERRAL_BONUS;
            case "ACTIVE_FRIEND" -> NotificationEvent.NotificationType.ACTIVE_FRIEND_REWARD;
            case "BACKLOG_COMPLETION" -> NotificationEvent.NotificationType.BACKLOG_COMPLETED;
            case "APP_DOWNLOAD" -> NotificationEvent.NotificationType.APP_DOWNLOAD_REWARD;
            case "VIDEO_VIEW" -> NotificationEvent.NotificationType.VIDEO_VIEW_REWARD;
            case "AD_VIEW" -> NotificationEvent.NotificationType.AD_VIEW_REWARD;
            default -> NotificationEvent.NotificationType.REFERRAL_BONUS;
        };
    }
}

