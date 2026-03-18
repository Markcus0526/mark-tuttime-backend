package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for updating notification campaign
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCampaignRequest {
    
    private String campaignName;
    
    private String sourceTitle;
    
    private String sourceMessage;
    
    private List<String> channels;
    
    private String priority;
}

