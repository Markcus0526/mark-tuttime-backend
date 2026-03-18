package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * User balances DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBalances {
    private BigDecimal apartBalance;
    private BigDecimal apBalance;
    private BigDecimal monthlyAccumulatedAp;
    private BigDecimal lifetimeAccumulatedAp;
}

