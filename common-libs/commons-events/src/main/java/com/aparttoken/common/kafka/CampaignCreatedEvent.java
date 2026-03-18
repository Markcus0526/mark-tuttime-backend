package com.tuttime.common.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Kafka event for notification campaign creation
 * Published by admin-service when admin creates a notification campaign
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignCreatedEvent {
    
    @JsonProperty("event_type")
    @Builder.Default
    private String eventType = "CAMPAIGN_CREATED";
    
    @JsonProperty("campaign_id")
    private Long campaignId;
    
    @JsonProperty("campaign_name")
    private String campaignName;
    
    @JsonProperty("template_key")
    private String templateKey; // Optional
    
    @JsonProperty("source_language")
    @Builder.Default
    private String sourceLanguage = "en";
    
    @JsonProperty("source_title")
    private String sourceTitle;
    
    @JsonProperty("source_message")
    private String sourceMessage;
    
    @JsonProperty("source_title_en")
    private String sourceTitleEn;
    
    @JsonProperty("source_message_en")
    private String sourceMessageEn;
    
    @JsonProperty("source_title_ru")
    private String sourceTitleRu;
    
    @JsonProperty("source_message_ru")
    private String sourceMessageRu;
    
    @JsonProperty("channels")
    private List<String> channels; // ['PUSH', 'EMAIL', 'IN_APP', 'SMS']
    
    @JsonProperty("target_audience")
    private String targetAudience; // 'INACTIVE_MINING', 'NEVER_STARTED_MINING', 'USER_GROUP', 'ALL_USERS', 'CUSTOM_CRITERIA'
    
    @JsonProperty("target_user_group_id")
    private Long targetUserGroupId; // If targetAudience is 'USER_GROUP'
    
    @JsonProperty("target_criteria")
    private Map<String, Object> targetCriteria; // JSONB criteria
    
    @JsonProperty("priority")
    @Builder.Default
    private String priority = "NORMAL"; // LOW, NORMAL, HIGH, URGENT
    
    @JsonProperty("created_by")
    private Long createdBy; // Admin ID
    
    @JsonProperty("timestamp")
    private String timestamp; // ISO 8601 format
    
    @JsonProperty("version")
    @Builder.Default
    private String version = "1.0";
}
