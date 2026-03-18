package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * App Version Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppVersionResponse {
    private String androidVersion;
    private String androidInstallLink;
    private String iosVersion;
    private String iosInstallLink;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime updatedAt;
    
    private Long updatedBy;
}

