package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;

/**
 * Response DTO for investor details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestorResponse {
    
    private Long investorId;
    private Long poolId;
    private Long userId;
    private String walletAddress;
    private BigDecimal allocationAmount;
    private BigDecimal claimedAmount; // Total amount claimed so far
    private BigDecimal availableAmount; // Amount available to claim now
    private BigDecimal lockedAmount; // Amount still locked (not yet claimable)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime vestingStartDate;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime vestingEndDate;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime cliffEndDate; // Date when cliff period ends
    private Integer vestingPeriodMonths;
    private Integer cliffPeriodMonths;
    private String claimStatus; // PENDING, CLAIMABLE, CLAIMED, EXPIRED
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lastClaimDate; // Last time investor claimed
    private Integer claimCount; // Number of times investor has claimed
    private String notes;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime updatedAt;
}

