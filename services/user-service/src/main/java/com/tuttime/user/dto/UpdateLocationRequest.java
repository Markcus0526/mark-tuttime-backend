package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for updating user location
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update user location", example = """
    {
      "latitude": 40.7128,
      "longitude": -74.0060,
      "accuracy": 50,
      "privacyLevel": "exact"
    }
    """)
public class UpdateLocationRequest {
    
    @Schema(description = "Latitude coordinate (-90 to 90)", example = "40.7128")
    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private BigDecimal latitude;

    @Schema(description = "Longitude coordinate (-180 to 180)", example = "-74.0060")
    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private BigDecimal longitude;

    @Schema(description = "Location accuracy in meters (optional)", example = "50")
    private Integer accuracy;

    @Schema(description = "Privacy level: exact (auto-detect mode only, optional)", 
            example = "exact", allowableValues = {"exact"})
    private String privacyLevel;
}

