package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for user social analytics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSocialAnalyticsResponse {
	private Long totalPosts;
	private Long totalFollowers;
	private Long totalFollowing;
	private Long totalLikesReceived;
	private Long totalCommentsReceived;
}

