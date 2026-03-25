package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Response DTO for video boost details
 *
 * <p>Video boosts are triggered by completed video views.
 * Each completed video view provides a mining speed boost for a configurable duration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoBoostResponse {

	private Long boostId;
	private Long userId;
	private Long videoId; // References content-service videos
	private BigDecimal boostAmount; // AP/hour boost amount
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime startedAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime expiresAt;
	private Boolean isActive;
	private Boolean isExpired; // Computed: expiresAt < now
	private Long daysRemaining; // Computed: days until expiration
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime updatedAt;
}

