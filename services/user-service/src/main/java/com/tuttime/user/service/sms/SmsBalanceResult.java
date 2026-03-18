package com.tuttime.user.service.sms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of an SMS provider balance check.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsBalanceResult {
    private double balance;
    private String currency;
    private String providerName;
}
