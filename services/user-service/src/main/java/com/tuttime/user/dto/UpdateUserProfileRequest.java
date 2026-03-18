package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating user profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 100, message = "Telegram ID must not exceed 100 characters")
    private String telegramId;

    @Size(max = 100, message = "Instagram username must not exceed 100 characters")
    private String instagram;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Schema(description = "User's gender", example = "MALE", allowableValues = {"MALE", "FEMALE", "OTHER", "PREFER_NOT_TO_SAY"})
    private com.tuttime.common.constants.Gender gender;

    @Size(max = 5000, message = "About section must not exceed 5000 characters")
    private String about;

    @Size(max = 1000, message = "Websites must not exceed 1000 characters")
    private String websites;

    @Size(max = 10, message = "Preferred language code must not exceed 10 characters")
    @Schema(description = "User preferred language code (e.g., en, ko, ja, zh-CN) for notification translation")
    private String preferredLanguage;
}

