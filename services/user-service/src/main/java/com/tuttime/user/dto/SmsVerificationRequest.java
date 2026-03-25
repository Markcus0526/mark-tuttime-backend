package com.tuttime.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SmsVerificationRequest {
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid phone number")
    @NotBlank
    private String phoneNumber;
}