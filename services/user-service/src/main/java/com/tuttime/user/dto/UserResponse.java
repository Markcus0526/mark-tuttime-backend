package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User information response")
public class UserResponse {
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String phone;
    private String telegram;
    private String instagram;
    private String country;
    private String city;
    private Boolean isVerified;
    
    @Schema(description = "Whether the user's email address is verified via email verification code")
    private Boolean emailVerified;
    
    private Boolean kycCompleted;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime kycVerifiedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime registrationDate;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lastActive;
    
    @Schema(description = "Last time the user logged in")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lastLogin;
    
    private Boolean isActive;
    private Boolean isBlocked;
    
    @Schema(description = "Reason why the account was blocked (if blocked)")
    private String blockedReason;
    
    @Schema(description = "Timestamp when the account was blocked")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime blockedAt;
    
    @Schema(description = "Timestamp when the record was created")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime createdAt;
    
    @Schema(description = "Timestamp when the record was last updated")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime updatedAt;
    
    @Schema(description = "Profile image URL (1024x1024px) - for full profile view")
    private String profileImageUrl;

    @Schema(description = "Profile image tiny thumbnail URL (32x32px) - for map markers, compact lists (~1-3KB)")
    private String profileImageThumbnailUrl;

    @Schema(description = "Profile image small thumbnail URL (128x128px) - for chat headers, profile cards (~5-10KB)")
    private String profileImageSmallUrl;

    @Schema(description = "Profile image medium thumbnail URL (512x512px) - for profile previews, modals (~30-50KB)")
    private String profileImageMediumUrl;

    @Schema(description = "Profile image URLs in different sizes - optimized for various use cases")
    private ProfileImageUrls profileImages;
    
    @Schema(description = "User's bio/about section")
    private String about;
    
    @Schema(description = "User's websites (comma-separated)")
    private String websites;
    
    // Location fields (only included if location sharing is enabled and privacy allows)
    @Schema(description = "Latitude coordinate (blurred based on privacy level, null if private)", example = "40.71")
    private BigDecimal latitude;
    
    @Schema(description = "Longitude coordinate (blurred based on privacy level, null if private)", example = "-74.00")
    private BigDecimal longitude;
    
    @Schema(description = "Location accuracy in meters")
    private Integer locationAccuracy;
    
    @Schema(description = "Timestamp when location was last updated")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime locationUpdatedAt;
    
    @Schema(description = "Location privacy level", example = "exact", allowableValues = {"exact", "private", "static"})
    private String locationPrivacyLevel;
    
    @Schema(description = "Whether location sharing is enabled", example = "true")
    private Boolean locationSharingEnabled;
    
    @Schema(description = "User status level", example = "BRONZE", allowableValues = {"BRONZE", "GOLD", "GOLD_PLUS", "PLATINUM", "PLATINUM_PLUS", "ELITE"})
    private String statusLevel;
    
    @Schema(description = "Total APART token balance")
    private BigDecimal totalApartBalance;
    
    @Schema(description = "Total AP Credits balance")
    private BigDecimal totalApBalance;
    
    @Schema(description = "Current mining speed in AP/hour")
    private BigDecimal currentMiningSpeed;
    
    @Schema(description = "Current AP balance")
    private BigDecimal currentApBalance;
    
    @Schema(description = "Total number of friends (referrals)")
    private Integer totalFriendsCount;
    
    @Schema(description = "User's gender", example = "MALE", allowableValues = {"MALE", "FEMALE", "OTHER", "PREFER_NOT_TO_SAY"})
    private String gender;
    
    @Schema(description = "User preferred language code (e.g., en, ko, ja, zh-CN) for notification translation", example = "en")
    private String preferredLanguage;
}

