package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin Update User Request DTO
 * 
 * <p>Used by admin to update user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateUserRequest {
    
    private String firstName;
    private String lastName;
    
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;
    
    @Email(message = "Invalid email format")
    private String email;
    
    private String phone;
    private String city;
    private String country;
    
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date of birth must be in YYYY-MM-DD format")
    private String dateOfBirth;
    
    @Schema(description = "User's gender", example = "MALE", allowableValues = {"MALE", "FEMALE", "OTHER", "PREFER_NOT_TO_SAY"})
    private com.tuttime.common.constants.Gender gender;
    
    private String telegramId; // Maps to telegram field in User entity
    
    @Size(max = 100, message = "Instagram username must not exceed 100 characters")
    private String instagram;
}

