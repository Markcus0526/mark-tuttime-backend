package com.tuttime.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for creating user group
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserGroupRequest {
    
    @NotBlank(message = "Group name is required")
    private String groupName;
    
    private String description;
    
    private Map<String, Object> criteria;
}

