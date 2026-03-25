package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Response DTO for blocked user information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedUserResponse {

	private Long userId;
	private String email;
	private String phone;
	private String blockedReason;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime blockedAt;
	private String blockedBy; // Admin who blocked the user
}

