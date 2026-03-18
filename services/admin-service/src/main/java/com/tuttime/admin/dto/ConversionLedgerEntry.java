package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Conversion Ledger Entry DTO
 * 
 * <p>Represents a single entry in the monthly conversion ledger,
 * showing user details and conversion amounts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionLedgerEntry {
    /** User ID */
    private Long userId;
    
    /** Username (first name + last name) */
    private String username;
    
    /** AP amount (AP Credits) */
    private BigDecimal apAmount;
    
    /** User share as decimal (0-1) */
    @JsonProperty("userShare")
    private BigDecimal userSharePercent;
    
    /** Distributed APART amount */
    private BigDecimal distributedApartAmount;
}

