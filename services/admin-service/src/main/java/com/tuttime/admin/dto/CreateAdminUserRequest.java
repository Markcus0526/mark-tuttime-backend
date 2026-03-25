package com.tuttime.admin.dto;

import com.tuttime.admin.entity.AdminUser;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create Admin User Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdminUserRequest {

	@NotBlank(message = "Username is required")
	@Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
	@Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
	private String username;

	@NotBlank(message = "Email is required")
	@Email(message = "Email must be valid")
	@Size(max = 255, message = "Email must not exceed 255 characters")
	private String email;

	@NotBlank(message = "Password is required")
	@Size(min = 8, message = "Password must be at least 8 characters")
	private String password;

	@NotNull(message = "Role is required")
	private AdminUser.AdminRole role;

	@Builder.Default
	private Boolean isActive = true;
}

