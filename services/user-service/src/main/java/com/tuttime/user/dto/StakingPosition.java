package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Staking position DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StakingPosition {
    private Long stakingId;
    private Long poolId;
    private BigDecimal amount;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime stakedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime unlockAt;
    private BigDecimal boostMultiplier;
    private BigDecimal rewardsEarned;
}

