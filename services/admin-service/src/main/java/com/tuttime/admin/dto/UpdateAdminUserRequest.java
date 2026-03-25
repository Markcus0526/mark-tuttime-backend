package com.tuttime.admin.dto;

import com.tuttime.admin.entity.AdminUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update Admin User Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAdminUserRequest {

	@Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
	@Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
	private String username;

	@Email(message = "Email must be valid")
	@Size(max = 255, message = "Email must not exceed 255 characters")
	private String email;

	private AdminUser.AdminRole role;

	private Boolean isActive;
}

