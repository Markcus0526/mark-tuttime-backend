package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for pausing user account
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PauseAccountRequest {
    private String reason;
    @Builder.Default
    private Boolean notifyUser = true;
}

