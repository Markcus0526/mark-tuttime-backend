package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Response DTO for video details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponse {

	private Long videoId;
	private Long advertiserUserId;
	private String title;
	private String description;
	private String videoUrl;
	private String thumbnailUrl;
	private String externalLink;
	private Integer durationSeconds;
	private String status; // PENDING, APPROVED, REJECTED, ACTIVE, PAUSED, COMPLETED
	private Integer totalViews;
	private Integer totalCompletedViews;
	private BigDecimal paymentAmount;
	private BigDecimal burnPercentage;
	private BigDecimal burnedAmount;
	private BigDecimal tokenCost;
	private BigDecimal boostAmount;
	private Integer boostDurationHours;
	private Boolean isActive;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime scheduledAt; // For scheduling video publication
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime approvedAt;
	private Long approvedBy;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime updatedAt;
}

