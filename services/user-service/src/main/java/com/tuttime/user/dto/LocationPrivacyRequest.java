package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for updating location privacy settings and location status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update location privacy settings", example = """
    {
      "sharingEnabled": true,
      "privacyLevel": "exact"
    }
    """)
public class LocationPrivacyRequest {
    
    @Schema(description = "Whether to enable location sharing", example = "true")
    @NotNull(message = "Sharing enabled status is required")
    private Boolean sharingEnabled;

    @Schema(description = "Privacy level: exact (auto-detect), private, or static (manual selection)", 
            example = "exact", 
            allowableValues = {"exact", "private", "static"})
    @NotNull(message = "Privacy level is required")
    private String privacyLevel;

    @Schema(description = "Latitude coordinate (required when privacyLevel is 'static')", example = "40.7128")
    private BigDecimal latitude;

    @Schema(description = "Longitude coordinate (required when privacyLevel is 'static')", example = "-74.0060")
    private BigDecimal longitude;
}

