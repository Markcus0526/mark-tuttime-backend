package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result DTO for password reset initiation
 * Contains information about the verification method used and where the code was sent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetInitiationResult {
    /**
     * Verification method used to send the code
     * Values: "Email", "Telegram Gateway", "Flash Call", "Green SMS", "Twilio"
     */
    private String verificationMethod;
    
    /**
     * Destination where the verification code was sent
     * Examples: email address, "your Telegram account", "flash call", "SMS"
     */
    private String sentTo;
    
    /**
     * Retry count (1-based): which attempt this is.
     * Useful for UI: "code sent via SMS, attempt 3"
     */
    private Integer retryCount;
}

