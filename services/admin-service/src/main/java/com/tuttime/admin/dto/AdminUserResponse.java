package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tuttime.admin.entity.AdminUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Admin User Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {

	private Long adminId;
	private String username;
	private String email;
	private AdminUser.AdminRole role;
	private Boolean isActive;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime lastLoginAt;

	private Integer failedLoginAttempts;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime lockedUntil;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime updatedAt;

	private Long createdBy;
	private String createdByUsername;
}

