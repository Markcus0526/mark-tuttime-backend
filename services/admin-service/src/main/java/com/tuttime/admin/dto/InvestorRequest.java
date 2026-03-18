package com.tuttime.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Request DTO for adding investors to vesting pools
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestorRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Allocation is required")
    private BigDecimal allocation; // Total tokens allocated to this investor
    
    @NotNull(message = "Vesting start date is required")
    private LocalDate vestingStartDate; // When vesting starts for this investor
    
    // Optional fields
    private String walletAddress; // Investor's wallet address for claiming (can be set later)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime vestingEndDate; // When vesting ends for this investor
    private Integer vestingPeriodMonths; // Override pool vesting period if needed
    private Integer cliffPeriodMonths; // Override pool cliff period if needed
    private String notes; // Admin notes about this investor
}

