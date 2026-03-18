package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for post analytics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostAnalyticsResponse {
    private Long totalPosts;
    private Long totalLikes;
    private Long totalComments;
    private Long activeUsers;
}

