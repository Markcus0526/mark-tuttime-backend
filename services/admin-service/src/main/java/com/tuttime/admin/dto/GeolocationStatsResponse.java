package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Geolocation Statistics Response DTO
 * 
 * <p>Represents aggregated statistics for a specific geographic location (country/city).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeolocationStatsResponse {
    private String country;
    private String city;
    private Long usersCount;
    private Long activeMinersCount;
    private BigDecimal averageApMiningSpeed;
    private BigDecimal averageApartReserve;
    private BigDecimal lastConversionDistributedAmount;
    private BigDecimal totalStakedAmount;
}

