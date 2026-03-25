package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Request DTO for creating/updating videos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoRequest {

	private Long advertiserUserId; // null for admin-created videos

	@NotBlank(message = "Title is required")
	private String title;

	// S3 file key for uploaded videos (set after S3 upload)
	private String s3FileKey;

	private String description;

	// Video URL is optional if uploading a file via S3
	// If videoUrl is provided, it will be used. Otherwise, file upload is expected.
	private String videoUrl;

	private String thumbnailUrl;

	private String externalLink;

	@NotNull(message = "Duration is required")
	private Integer durationSeconds;

	private String status; // PENDING, APPROVED, REJECTED, ACTIVE, PAUSED, COMPLETED

	private BigDecimal paymentAmount;

	private BigDecimal burnPercentage;

	private BigDecimal tokenCost; // Cost to advertiser in APART tokens

	private BigDecimal boostAmount; // AP/hour bonus for watching video

	private Integer boostDurationHours; // How long boost lasts (hours)

	private Boolean isActive; // Whether video is active and available

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime scheduledPublishDate; // For scheduling video publication
}

