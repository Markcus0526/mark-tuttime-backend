package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Response DTO for hashtag details (admin view)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HashtagResponse {
    
    private Long hashtagId;
    private String tag;
    private Long postCount;
    private OffsetDateTime createdAt;
}

