package com.tuttime.restaurant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Admin User Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantResponse {

    private Long restaurantId;
    private String name;
    private String email;
    private String phone;
    private String telegram;
    private String country;
    private String city;
    private String blockedReason;

    private Boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lastLoginAt;

    private Integer failedLoginAttempts;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lockedUntil;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime blockedAt;

    @Schema(description = "About image URL (1024x1024px) - for full profile view")
    private String aboutImageUrl;

    @Schema(description = "About image tiny thumbnail URL (32x32px) - for map markers, compact lists (~1-3KB)")
    private String aboutImageThumbnailUrl;

    @Schema(description = "About image small thumbnail URL (128x128px) - for chat headers, profile cards (~5-10KB)")
    private String aboutImageSmallUrl;

    @Schema(description = "About image medium thumbnail URL (512x512px) - for profile previews, modals (~30-50KB)")
    private String aboutImageMediumUrl;

    @Schema(description = "About image URLs in different sizes - optimized for various use cases")
    private AboutImageUrls aboutImages;

    private Long createdBy;
    private String createdByUsername;
}

