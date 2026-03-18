package com.tuttime.common.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Kafka event for notification template creation/update
 * Published by admin-service when admin creates or updates a template
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateCreatedEvent {
    
    @JsonProperty("event_type")
    @Builder.Default
    private String eventType = "TEMPLATE_CREATED";
    
    @JsonProperty("template_id")
    private Long templateId;
    
    @JsonProperty("template_key")
    private String templateKey;
    
    @JsonProperty("category")
    private String category; // USER_TO_USER, MINING, SYSTEM, etc.
    
    @JsonProperty("notification_type")
    private String notificationType;
    
    @JsonProperty("source_language")
    @Builder.Default
    private String sourceLanguage = "en";
    
    @JsonProperty("source_title")
    private String sourceTitle;
    
    @JsonProperty("source_message")
    private String sourceMessage;
    
    @JsonProperty("variables")
    private List<String> variables; // ['senderUsername', 'userId', etc.]
    
    @JsonProperty("is_system")
    @Builder.Default
    private Boolean isSystem = false;
    
    @JsonProperty("created_by")
    private Long createdBy; // Admin ID
    
    @JsonProperty("timestamp")
    private String timestamp; // ISO 8601 format
    
    @JsonProperty("version")
    @Builder.Default
    private String version = "1.0";
}
