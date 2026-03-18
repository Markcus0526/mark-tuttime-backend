package com.tuttime.admin.dto;

import com.tuttime.admin.entity.AdminUser;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Admin User Summary Response DTO (for list views)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserSummaryResponse {

    private Long adminId;
    private String username;
    private String email;
    private AdminUser.AdminRole role;
    private Boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lastLoginAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime createdAt;
}

