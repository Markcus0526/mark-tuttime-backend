package com.tuttime.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating FCM token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFcmTokenRequest {
    
    /**
     * FCM registration token from Firebase SDK
     */
    @NotBlank(message = "FCM token is required")
    private String fcmToken;
    
    /**
     * Client device ID (optional, used to identify the device)
     */
    private String deviceId;
}

