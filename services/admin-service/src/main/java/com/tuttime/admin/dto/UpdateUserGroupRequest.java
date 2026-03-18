package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for updating user group
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserGroupRequest {
    
    private String groupName;
    
    private String description;
    
    private Map<String, Object> criteria;
}
