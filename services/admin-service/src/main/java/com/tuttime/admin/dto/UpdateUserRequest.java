package com.tuttime.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update User Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
	@Size(max = 100, message = "First name must not exceed 100 characters")
	private String firstName;

	@Size(max = 100, message = "Last name must not exceed 100 characters")
	private String lastName;

	@Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
	private String username;

	@Email(message = "Email must be valid")
	@Size(max = 255, message = "Email must not exceed 255 characters")
	private String email;

	@Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone must be a valid international format")
	@Size(max = 20, message = "Phone must not exceed 20 characters")
	private String phone;

	@Size(max = 100, message = "Telegram must not exceed 100 characters")
	private String telegram;

	@Size(max = 100, message = "Country must not exceed 100 characters")
	private String country;

	@Size(max = 100, message = "City must not exceed 100 characters")
	private String city;

	@Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "Date of birth must be in YYYY-MM-DD format")
	private String dateOfBirth;

	private String gender;

	@Size(max = 100, message = "Instagram must not exceed 100 characters")
	private String instagram;
}

