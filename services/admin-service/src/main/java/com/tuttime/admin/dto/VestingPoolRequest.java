package com.tuttime.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Request DTO for creating/updating vesting pools
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VestingPoolRequest {
    
    @NotBlank(message = "Pool name is required")
    private String poolName;
    
    @NotNull(message = "Total amount is required")
    private BigDecimal totalAmount; // Total tokens allocated to this pool
    
    @NotBlank(message = "Vesting schedule is required")
    private String vestingSchedule; // LINEAR, CLIFF, CUSTOM
    
    // Optional fields for updates
    private String description;
    private String contractAddress; // EVM contract address (set after contract deployment)
    private BigDecimal currentBalance; // Current token balance in pool
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime startDate; // Vesting start date
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime endDate; // Vesting end date
    private Integer vestingPeriodMonths; // Vesting period in months
    private Integer cliffPeriodMonths; // Cliff period in months (no claims before this)
    private Boolean isActive; // Whether pool is active
    private String metadata; // Additional metadata as JSON string
}

