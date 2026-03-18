package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for signup token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupTokenResponse {
    private String signupToken;
}

