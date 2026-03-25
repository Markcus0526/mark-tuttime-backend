package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response DTO for post details (admin view)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

	private Long postId;
	private UserSummaryResponse author;
	private String caption;
	private List<MediaDto> media;
	private String locationName;
	private java.math.BigDecimal latitude;
	private java.math.BigDecimal longitude;
	private Long likeCount;
	private Long commentCount;
	private Boolean isLiked;
	private Boolean isSaved;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;
}

