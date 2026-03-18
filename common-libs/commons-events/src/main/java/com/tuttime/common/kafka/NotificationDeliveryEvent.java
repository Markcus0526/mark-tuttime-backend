package com.tuttime.common.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Kafka event for notification delivery (per language)
 * Published by Notification Orchestrator after translation
 * Consumed by Delivery Workers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDeliveryEvent {
    
    @JsonProperty("event_type")
    @Builder.Default
    private String eventType = "NOTIFICATION_DELIVERY";
    
    @JsonProperty("campaign_id")
    private Long campaignId;
    
    @JsonProperty("language_code")
    private String languageCode; // 'en', 'ko', 'ru', etc.
    
    @JsonProperty("channels")
    private List<String> channels; // ['PUSH', 'EMAIL', 'IN_APP', 'SMS']
    
    @JsonProperty("target_audience")
    private String targetAudience;
    
    @JsonProperty("target_user_group_id")
    private Long targetUserGroupId; // If targetAudience is 'USER_GROUP'
    
    @JsonProperty("target_criteria")
    private Map<String, Object> targetCriteria;
    
    @JsonProperty("priority")
    @Builder.Default
    private String priority = "NORMAL";
    
    @JsonProperty("timestamp")
    private String timestamp; // ISO 8601 format
    
    @JsonProperty("template_id")
    private Long templateId; // Optional: if using a template
    
    @JsonProperty("translated_title")
    private String translatedTitle; // Pre-translated title for this language
    
    @JsonProperty("translated_message")
    private String translatedMessage; // Pre-translated message for this language
    
    @JsonProperty("template_variables")
    private Map<String, String> templateVariables; // Variables for template rendering
    
    @JsonProperty("target_user_ids")
    private List<Long> targetUserIds; // Batch of user IDs for this delivery event
    
    @JsonProperty("notification_type")
    private String notificationType; // Original notification type
    
    @JsonProperty("reference_type")
    private String referenceType;
    
    @JsonProperty("reference_id")
    private String referenceId;
    
    @JsonProperty("version")
    @Builder.Default
    private String version = "1.0";
}
