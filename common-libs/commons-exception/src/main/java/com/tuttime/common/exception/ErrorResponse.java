package com.tuttime.common.exception;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Standardized error response format for all services.
 * 
 * <p>This class provides a consistent structure for error responses across
 * all microservices, including correlation ID for distributed tracing,
 * error codes for client handling, message keys for internationalization,
 * and additional details for field-level errors.
 * 
 * <p><b>Error Code vs Message Key:</b>
 * <ul>
 *   <li><b>errorCode:</b> Semantic error code for programmatic handling (e.g., BUSINESS_RULE_VIOLATION)</li>
 *   <li><b>messageKey:</b> Translation key for frontend internationalization (e.g., PHONE_NUMBER_REQUIRED)</li>
 *   <li><b>message:</b> Default English message (fallback if translation not available)</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime timestamp;
    private int status;
    private String error;
    private String errorCode;
    private String messageKey;  // Translation key for i18n (e.g., "PHONE_NUMBER_REQUIRED")
    private String message;     // Default English message
    private String path;
    private String correlationId;
    private String traceId;
    private Map<String, Object> details;
}
