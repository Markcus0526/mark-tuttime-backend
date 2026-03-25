package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * User Summary Response DTO for admin panel
 * Contains all fields from the users table for comprehensive list view
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)  // Include all fields even if null
public class UserSummaryResponse {
	// Basic Information
	private Long userId;
	private String email;
	private String username;
	private String firstName;
	private String lastName;
	private String phone;
	private String telegram;
	private String profileImageUrl;

	// Location Information
	private String country;
	private String city;
	private BigDecimal latitude;
	private BigDecimal longitude;
	private Integer locationAccuracy;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime locationUpdatedAt;
	private String locationPrivacyLevel;
	private Boolean locationSharingEnabled;

	// Verification & Status
	private Boolean isVerified;
	private Boolean emailVerified;
	private Boolean kycCompleted;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime kycVerifiedAt;
	private String status; // ACTIVE, BLOCKED, INACTIVE, PAUSED
	private String kycStatus; // PENDING, APPROVED, REJECTED
	private Boolean isActive;
	private Boolean isBlocked;
	private String blockedReason;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime blockedAt;

	// Account Management
	private Boolean isPaused;
	private Boolean withdrawalFrozen;
	private Boolean antiFraudWatch;

	// Timestamps
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime registrationDate;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime lastActive;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime lastLogin;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime updatedAt;

	// Balances (from other services - may be null in list view)
	private Long apBalance;
	private Long apartBalance;
}

