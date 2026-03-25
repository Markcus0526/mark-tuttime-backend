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
 * Request DTO for creating/updating ads
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdRequest {

	private Long advertiserUserId; // null for admin-created ads

	@NotBlank(message = "Ad title is required")
	private String adTitle;

	private String adDescription;

	@NotBlank(message = "Ad type is required")
	private String adType;

	private String contentUrl;

	private String externalLink;

	private String targetUrl;

	@NotNull(message = "Payment amount is required")
	private BigDecimal paymentAmount;

	private BigDecimal burnPercentage;

	private String status; // DRAFT, PENDING, APPROVED, REJECTED, ACTIVE, PAUSED, COMPLETED, CANCELLED

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime startDate;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime endDate;

	private Boolean isHideable;

	private BigDecimal boostPerView;
}

