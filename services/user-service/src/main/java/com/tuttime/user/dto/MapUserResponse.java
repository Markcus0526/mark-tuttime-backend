package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for user information on map
 * 
 * <p>Contains minimal user information for map markers.
 * Coordinates may be blurred based on privacy level.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User information for map display")
public class MapUserResponse {
    
    @Schema(description = "User ID", example = "123")
    private Long userId;

    @Schema(description = "Username", example = "johndoe")
    private String username;

    @Schema(description = "First name", example = "John")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "Latitude coordinate (may be blurred based on privacy)", example = "40.7128")
    private BigDecimal latitude;

    @Schema(description = "Longitude coordinate (may be blurred based on privacy)", example = "-74.0060")
    private BigDecimal longitude;

    @Schema(description = "City name", example = "New York")
    private String city;

    @Schema(description = "Country name", example = "USA")
    private String country;

    @Schema(description = "Profile image thumbnail URL (32x32px) - optimized for map markers (~1-3KB)", example = "https://...")
    private String profileImageThumbnailUrl;

    @Schema(description = "Profile image original URL (1024x1024px) - for full profile view", example = "https://...")
    private String profileImageUrl;

    @Schema(description = "Whether user is actively mining", example = "true")
    private Boolean miningActive;

    @Schema(description = "User status level", example = "GOLD")
    private String statusLevel;

    @Schema(description = "Privacy level", example = "exact", allowableValues = {"exact", "private", "static"})
    private String privacyLevel;
}

