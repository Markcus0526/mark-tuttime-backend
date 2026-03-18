package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Response DTO for blocked device information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedDeviceResponse {
    
    private String deviceId;
    private String reason;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime blockedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime expiresAt; // null for permanent blocks
    private Boolean isPermanent;
}

