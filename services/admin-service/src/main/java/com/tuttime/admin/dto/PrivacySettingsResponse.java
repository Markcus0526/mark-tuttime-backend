package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Response DTO for user privacy settings (admin view)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacySettingsResponse {

	private Long userId;
	private String accountPrivacy; // PUBLIC, PRIVATE
	private String commentRestriction; // EVERYONE, FOLLOWERS, NONE
	private Boolean hideLikes;
	private Boolean hideFollowers;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;
}

