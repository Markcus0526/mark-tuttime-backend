package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Response DTO for whitelisted IP address information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhitelistedIpResponse {
    
    private String ipAddress;
    private String reason; // Optional reason for whitelisting
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime whitelistedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime expiresAt; // null for permanent whitelist
    private Boolean isPermanent;
}

