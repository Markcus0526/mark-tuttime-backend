package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;

    private String firstName;

    private String lastName;

    private String phone;

    private String telegram;

    private String instagram;

    private String country;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Date of birth is required")
    private String dateOfBirth;

    @Schema(description = "User's gender", example = "MALE", allowableValues = {"MALE", "FEMALE", "OTHER", "PREFER_NOT_TO_SAY"})
    private com.tuttime.common.constants.Gender gender;

    private String referralCode;

    @Schema(description = "Latitude coordinate (optional, required if allowing location detection)", example = "40.7128")
    private java.math.BigDecimal latitude;

    @Schema(description = "Longitude coordinate (optional, required if allowing location detection)", example = "-74.0060")
    private java.math.BigDecimal longitude;

    @Schema(description = "Location accuracy in meters (optional)", example = "50")
    private Integer locationAccuracy;

    @Schema(description = "List of user interests (optional, max 20 interests)", example = "[\"IT\", \"Бизнес\", \"Путешествия\"]")
    @Size(max = 20, message = "Maximum 20 interests allowed")
    private java.util.List<@Size(min = 1, max = 100, message = "Each interest must be between 1 and 100 characters") String> interests;
}

