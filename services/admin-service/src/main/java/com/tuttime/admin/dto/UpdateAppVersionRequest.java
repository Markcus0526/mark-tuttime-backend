package com.tuttime.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update App Version Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAppVersionRequest {

	@NotBlank(message = "Android version is required")
	@Size(max = 50, message = "Android version must not exceed 50 characters")
	private String androidVersion;

	@NotBlank(message = "Android install link is required")
	private String androidInstallLink;

	@NotBlank(message = "iOS version is required")
	@Size(max = 50, message = "iOS version must not exceed 50 characters")
	private String iosVersion;

	@NotBlank(message = "iOS install link is required")
	private String iosInstallLink;
}

