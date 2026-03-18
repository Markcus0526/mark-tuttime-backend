package com.tuttime.user.service.sms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of an SMS send operation.
 * Contains request_id for providers that support delivery status polling (e.g. Green SMS).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsSendResult {
    /**
     * Provider-specific request/message ID for status polling.
     * Empty for providers that don't support delivery verification (e.g. Twilio Verify, Telegram).
     */
    private String requestId;
    
    /**
     * Provider name (e.g. "Green SMS", "Twilio").
     */
    private String providerName;
}
