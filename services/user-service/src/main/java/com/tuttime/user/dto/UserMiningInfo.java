package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * User mining information DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMiningInfo {
    private Boolean isActive;
    private BigDecimal miningSpeed;
    private String statusLevel;
    private BigDecimal statusBonusApPerHour;
    private Long totalLogsCount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lastLogAt;
}

