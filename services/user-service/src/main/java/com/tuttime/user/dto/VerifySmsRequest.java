package com.tuttime.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for SMS verification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifySmsRequest {
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank(message = "SMS code is required")
    @Size(min = 4, max = 4, message = "SMS code must be 4 digits")
    @Pattern(regexp = "^\\d{4}$", message = "SMS code must be 4 digits")
    private String smsCode;
}

