package com.tuttime.admin.kafka;

import com.tuttime.common.kafka.TemplateCreatedEvent;
import com.tuttime.common.kafka.TemplateUpdatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer for Template Events
 * Publishes template creation/update events to notification-service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String TEMPLATE_CREATED_TOPIC = "notification.template.created";
    private static final String TEMPLATE_UPDATED_TOPIC = "notification.template.updated";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /**
     * Publishes template created event to Kafka
     */
    public void sendTemplateCreated(TemplateCreatedEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            String partitionKey = event.getTemplateId() != null ? event.getTemplateId().toString() : null;
            
            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(TEMPLATE_CREATED_TOPIC, partitionKey, message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Template created event sent successfully: templateId={}, templateKey={}, offset={}",
                            event.getTemplateId(), event.getTemplateKey(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send template created event: templateId={}, templateKey={}",
                            event.getTemplateId(), event.getTemplateKey(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error serializing template created event: templateId={}, templateKey={}",
                    event.getTemplateId(), event.getTemplateKey(), e);
        }
    }
    
    /**
     * Publishes template updated event to Kafka
     */
    public void sendTemplateUpdated(TemplateUpdatedEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            String partitionKey = event.getTemplateId() != null ? event.getTemplateId().toString() : null;
            
            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(TEMPLATE_UPDATED_TOPIC, partitionKey, message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Template updated event sent successfully: templateId={}, templateKey={}, offset={}",
                            event.getTemplateId(), event.getTemplateKey(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send template updated event: templateId={}, templateKey={}",
                            event.getTemplateId(), event.getTemplateKey(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error serializing template updated event: templateId={}, templateKey={}",
                    event.getTemplateId(), event.getTemplateKey(), e);
        }
    }
    
    /**
     * Helper method to create and send template created event
     */
    public void sendTemplateCreated(
            Long templateId,
            String templateKey,
            String category,
            String notificationType,
            String sourceLanguage,
            String sourceTitle,
            String sourceMessage,
            List<String> variables,
            Boolean isSystem,
            Long createdBy) {
        
        TemplateCreatedEvent event = TemplateCreatedEvent.builder()
                .templateId(templateId)
                .templateKey(templateKey)
                .category(category)
                .notificationType(notificationType)
                .sourceLanguage(sourceLanguage != null ? sourceLanguage : "en")
                .sourceTitle(sourceTitle)
                .sourceMessage(sourceMessage)
                .variables(variables)
                .isSystem(isSystem != null ? isSystem : false)
                .createdBy(createdBy)
                .timestamp(OffsetDateTime.now().format(ISO_FORMATTER))
                .build();
        
        sendTemplateCreated(event);
    }
    
    /**
     * Helper method to create and send template updated event
     */
    public void sendTemplateUpdated(
            Long templateId,
            String templateKey,
            String sourceTitle,
            String sourceMessage,
            List<String> variables,
            Boolean isActive,
            Long updatedBy) {
        
        TemplateUpdatedEvent event = TemplateUpdatedEvent.builder()
                .templateId(templateId)
                .templateKey(templateKey)
                .sourceTitle(sourceTitle)
                .sourceMessage(sourceMessage)
                .variables(variables)
                .isActive(isActive)
                .updatedBy(updatedBy)
                .timestamp(OffsetDateTime.now().format(ISO_FORMATTER))
                .build();
        
        sendTemplateUpdated(event);
    }
}
