package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Token Stats Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenStatsResponse {
    private BigDecimal totalSupply;
    private BigDecimal totalCirculating;
    private BigDecimal totalStaked;
    private BigDecimal totalBurned;
    private Long totalHolders;
    private Long activeStakers;
}

