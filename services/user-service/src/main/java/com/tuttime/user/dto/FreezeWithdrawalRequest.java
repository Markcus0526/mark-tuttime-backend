package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for freezing user withdrawals
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreezeWithdrawalRequest {
    private String reason;
    @Builder.Default
    private Boolean notifyUser = true;
}

