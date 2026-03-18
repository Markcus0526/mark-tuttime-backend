package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for simple message responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Message response with verification method and user instructions")
public class MessageResponse {
    @Schema(description = "User-friendly message with instructions on where to find the verification code", 
            example = "Verification code sent successfully. Please check your Telegram messages for the code.")
    private String message;
    
    @Schema(description = "Verification method used to send the code", 
            example = "Telegram Gateway", 
            allowableValues = {"Telegram Gateway", "Flash Call", "Green SMS", "Twilio"})
    private String verificationMethod;
    
    @Schema(description = "Verification type code: 0 = Telegram, 1 = Flash Call, 2 = Others (SMS)", 
            example = "0",
            allowableValues = {"0", "1", "2"})
    private Integer verificationType;
    
    @Schema(description = "Destination where the verification code was sent. For email, shows the email address. For SMS/Telegram/Flash Call, shows a generic description.", 
            example = "example@gmail.com",
            allowableValues = {"email address", "your Telegram account", "flash call", "SMS", "email"})
    private String sentTo;
    
    @Schema(description = "Email address where the verification code was sent (only included when verificationType is 2 and code was sent via email)", 
            example = "user@example.com")
    private String email;
    
    @Schema(description = "Retry count (1-based): which attempt this is. Useful for UI: 'code sent via SMS, attempt 3'", 
            example = "1")
    private Integer retryCount;
}

