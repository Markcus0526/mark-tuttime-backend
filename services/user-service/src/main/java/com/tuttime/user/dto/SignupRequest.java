package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for user signup
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User signup request with signup token from SMS verification", example = """
    {
      "signUpToken": "***REMOVED***",
      "phoneNumber": "+1234567890",
      "password": "***REMOVED***",
      "username": "johndoe",
      "email": "john.doe@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "country": "USA",
      "city": "New York",
      "telegramId": "@johndoe"
    }
    """)
public class SignupRequest {
    @Schema(description = "Signup token obtained from SMS verification", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", required = true)
    @NotBlank(message = "Signup token is required")
    private String signUpToken;

    @Schema(description = "Phone number in E.164 format", example = "+1234567890", required = true)
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;

    @Schema(description = "User password (minimum 6 characters)", example = "SecurePass123!", required = true)
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Schema(description = "User's username (required, 3-100 characters)", example = "johndoe", required = true)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;

    @Schema(description = "User's first name", example = "John")
    private String firstName;

    @Schema(description = "User's last name", example = "Doe")
    private String lastName;

    @Schema(description = "User's country", example = "USA")
    private String country;

    @Schema(description = "User's city", example = "New York", required = true)
    @NotBlank(message = "City is required")
    private String city;

    @Schema(description = "User's date of birth (YYYY-MM-DD format)", example = "1990-01-01", required = true)
    @NotBlank(message = "Date of birth is required")
    private String dateOfBirth;

    @Schema(description = "User's gender", example = "MALE", allowableValues = {"MALE", "FEMALE", "OTHER", "PREFER_NOT_TO_SAY"})
    private com.tuttime.common.constants.Gender gender;

    @Schema(description = "User's email address", example = "john.doe@example.com", required = true)
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "User's Telegram ID", example = "@johndoe")
    private String telegramId;

    @Schema(description = "User's Instagram username", example = "johndoe")
    @Size(max = 100, message = "Instagram username must not exceed 100 characters")
    private String instagram;

    @Schema(description = "Referral code from the user who referred this user (optional)", example = "A1B2C3D4")
    @Size(max = 50, message = "Referral code must not exceed 50 characters")
    private String referralCode;

    @Schema(description = "List of user interests (optional, max 20 interests)", example = "[\"IT\", \"Бизнес\", \"Путешествия\"]")
    @Size(max = 20, message = "Maximum 20 interests allowed")
    private java.util.List<@Size(min = 1, max = 100, message = "Each interest must be between 1 and 100 characters") String> interests;
}

