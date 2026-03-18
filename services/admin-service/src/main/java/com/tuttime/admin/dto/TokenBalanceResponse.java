package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Token Balance Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenBalanceResponse {
    private Long userId;
    private String username;
    private BigDecimal balance;
    private BigDecimal lockedAmount;
    private BigDecimal availableBalance;
}

