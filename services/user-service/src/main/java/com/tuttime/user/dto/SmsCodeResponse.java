package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for SMS verification code
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsCodeResponse {
    private String phoneNumber;
    private String smsCode;
}

