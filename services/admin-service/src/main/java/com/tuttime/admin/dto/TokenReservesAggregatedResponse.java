package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Token Reserves Aggregated Response DTO
 * 
 * <p>Aggregated token reserves across all networks.
 * This represents the total reserves, price, supply, and NAV.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenReservesAggregatedResponse {
    
    /**
     * Total treasury balance across all networks
     */
    private BigDecimal treasuryBalance;
    
    /**
     * Total hot wallet balance across all networks
     */
    private BigDecimal hotWalletBalance;
    
    /**
     * Current token price (should be the same across networks, or average)
     */
    private BigDecimal tokenPrice;
    
    /**
     * Total token supply
     */
    private BigDecimal totalSupply;
    
    /**
     * Net Asset Value (NAV) - calculated as totalSupply * tokenPrice
     */
    private BigDecimal nav;
}

