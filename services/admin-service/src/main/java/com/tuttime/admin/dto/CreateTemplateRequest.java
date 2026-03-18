package com.tuttime.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating notification template
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTemplateRequest {
    
    @NotBlank(message = "Template key is required")
    private String templateKey;
    
    @NotBlank(message = "Category is required")
    private String category;
    
    @NotBlank(message = "Notification type is required")
    private String notificationType;
    
    private String sourceLanguage;
    
    @NotBlank(message = "Source title is required")
    private String sourceTitle;
    
    @NotBlank(message = "Source message is required")
    private String sourceMessage;
    
    private List<String> variables;
    
    private Boolean isSystem;
}

