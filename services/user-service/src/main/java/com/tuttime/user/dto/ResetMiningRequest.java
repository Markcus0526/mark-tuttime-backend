package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for resetting user mining
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetMiningRequest {
    private String reason;
    @Builder.Default
    private Boolean resetMonthlyAccumulated = true;
}

