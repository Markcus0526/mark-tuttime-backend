package com.aparttoken.common.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Kafka event for notification template update
 * Published by admin-service when admin updates a template
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateUpdatedEvent {
    
    @JsonProperty("event_type")
    @Builder.Default
    private String eventType = "TEMPLATE_UPDATED";
    
    @JsonProperty("template_id")
    private Long templateId;
    
    @JsonProperty("template_key")
    private String templateKey;
    
    @JsonProperty("source_title")
    private String sourceTitle; // Optional - only if updated
    
    @JsonProperty("source_message")
    private String sourceMessage; // Optional - only if updated
    
    @JsonProperty("variables")
    private List<String> variables; // Optional - only if updated
    
    @JsonProperty("is_active")
    private Boolean isActive; // Optional - only if updated
    
    @JsonProperty("updated_by")
    private Long updatedBy; // Admin ID
    
    @JsonProperty("timestamp")
    private String timestamp; // ISO 8601 format
    
    @JsonProperty("version")
    @Builder.Default
    private String version = "1.0";
}
