package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating user privacy settings (admin)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePrivacySettingsRequest {
    
    private String accountPrivacy; // PUBLIC, PRIVATE
    private String commentRestriction; // EVERYONE, FOLLOWERS, NONE
    private Boolean hideLikes;
    private Boolean hideFollowers;
}

