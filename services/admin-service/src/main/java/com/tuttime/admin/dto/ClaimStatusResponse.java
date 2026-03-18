package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response DTO for investor claim status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimStatusResponse {
    
    private Long investorId;
    private Long poolId;
    private Long userId;
    private String walletAddress;
    private BigDecimal totalAllocation;
    private BigDecimal claimedAmount;
    private BigDecimal availableAmount;
    private BigDecimal lockedAmount;
    private String claimStatus;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime vestingStartDate;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime vestingEndDate;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime cliffEndDate;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime nextClaimDate; // Next date when more tokens become claimable
    private BigDecimal nextClaimAmount; // Amount that will be claimable on next claim date
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lastClaimDate;
    private Integer claimCount;
    private List<ClaimHistoryEntry> claimHistory; // History of all claims
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClaimHistoryEntry {
        private Long claimId;
        private BigDecimal amount;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
        private OffsetDateTime claimDate;
        private String transactionHash; // EVM transaction hash
        private String status; // PENDING, CONFIRMED, FAILED
    }
}

