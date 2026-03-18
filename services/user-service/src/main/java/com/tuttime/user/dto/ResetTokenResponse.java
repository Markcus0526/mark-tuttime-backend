package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for password reset token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetTokenResponse {
    private String resetToken;
}

