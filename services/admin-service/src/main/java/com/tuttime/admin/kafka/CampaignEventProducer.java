package com.tuttime.admin.kafka;

import com.tuttime.common.kafka.CampaignCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer for Campaign Events
 * Publishes campaign creation events to notification-service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String CAMPAIGN_CREATED_TOPIC = "notification.campaign.created";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /**
     * Publishes campaign created event to Kafka
     * 
     * @param event Campaign created event
     */
    public void sendCampaignCreated(CampaignCreatedEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            
            // Use campaignId as partition key for ordering
            String partitionKey = event.getCampaignId() != null ? event.getCampaignId().toString() : null;
            
            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(CAMPAIGN_CREATED_TOPIC, partitionKey, message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Campaign created event sent successfully: campaignId={}, offset={}",
                            event.getCampaignId(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send campaign created event: campaignId={}",
                            event.getCampaignId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error serializing campaign created event: campaignId={}",
                    event.getCampaignId(), e);
        }
    }
    
    /**
     * Helper method to create and send campaign created event
     */
    public void sendCampaignCreated(
            Long campaignId,
            String campaignName,
            String templateKey,
            String sourceLanguage,
            String sourceTitle,
            String sourceMessage,
            String sourceTitleEn,
            String sourceMessageEn,
            String sourceTitleRu,
            String sourceMessageRu,
            java.util.List<String> channels,
            String targetAudience,
            Long targetUserGroupId,
            java.util.Map<String, Object> targetCriteria,
            String priority,
            Long createdBy) {
        
        CampaignCreatedEvent event = CampaignCreatedEvent.builder()
                .campaignId(campaignId)
                .campaignName(campaignName)
                .templateKey(templateKey)
                .sourceLanguage(sourceLanguage != null ? sourceLanguage : "en")
                .sourceTitle(sourceTitle)
                .sourceMessage(sourceMessage)
                .sourceTitleEn(sourceTitleEn)
                .sourceMessageEn(sourceMessageEn)
                .sourceTitleRu(sourceTitleRu)
                .sourceMessageRu(sourceMessageRu)
                .channels(channels)
                .targetAudience(targetAudience)
                .targetUserGroupId(targetUserGroupId)
                .targetCriteria(targetCriteria)
                .priority(priority != null ? priority : "NORMAL")
                .createdBy(createdBy)
                .timestamp(OffsetDateTime.now().format(ISO_FORMATTER))
                .build();
        
        sendCampaignCreated(event);
    }
}

