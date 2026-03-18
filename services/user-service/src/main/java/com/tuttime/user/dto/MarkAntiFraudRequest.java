package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for marking user for anti-fraud watch
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkAntiFraudRequest {
    private String reason;
    @Builder.Default
    private String severity = "MEDIUM"; // LOW, MEDIUM, HIGH, CRITICAL
}

