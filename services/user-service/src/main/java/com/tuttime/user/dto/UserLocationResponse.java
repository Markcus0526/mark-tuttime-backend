package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Response DTO for user location information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User location information")
public class UserLocationResponse {
    
    @Schema(description = "Latitude coordinate", example = "40.7128")
    private BigDecimal latitude;

    @Schema(description = "Longitude coordinate", example = "-74.0060")
    private BigDecimal longitude;

    @Schema(description = "City name", example = "New York")
    private String city;

    @Schema(description = "Country name", example = "USA")
    private String country;

    @Schema(description = "Privacy level", example = "exact", allowableValues = {"exact", "private", "static"})
    private String privacyLevel;

    @Schema(description = "Whether location sharing is enabled", example = "true")
    private Boolean sharingEnabled;

    @Schema(description = "Location accuracy in meters", example = "50")
    private Integer accuracy;

    @Schema(description = "Timestamp when location was last updated")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime updatedAt;
}

