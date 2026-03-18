package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Account status DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatus {
    private Boolean isActive;
    private Boolean isBlocked;
    private Boolean isPaused;
    private String blockedReason;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime blockedAt;
    private Boolean kycCompleted;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime kycVerifiedAt;
    private Boolean withdrawalFrozen;
    private Boolean antiFraudWatch;
}

