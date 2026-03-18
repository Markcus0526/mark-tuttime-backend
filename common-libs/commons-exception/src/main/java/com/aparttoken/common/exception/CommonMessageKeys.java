package com.aparttoken.common.exception;

/**
 * Common message keys shared across multiple microservices.
 * 
 * <p>Only include keys that are used by 2+ services.
 * Service-specific keys should be in each service's own MessageKeys class.
 * 
 * <p><b>Naming Convention:</b>
 * <ul>
 *   <li>Common keys: No prefix (e.g., {@code UNKNOWN_ERROR})</li>
 *   <li>Service-specific keys: Use service prefix (e.g., {@code USER.PHONE_NUMBER_REQUIRED})</li>
 * </ul>
 * 
 * <p><b>Usage:</b>
 * <pre>
 * throw new BusinessException("Service unavailable", CommonMessageKeys.SERVICE_UNAVAILABLE);
 * </pre>
 * 
 * <p><b>Frontend Translation:</b>
 * Frontend should use these keys to look up translations:
 * <pre>
 * i18n.t(`errors.${messageKey}`, { locale: 'en' })
 * // e.g., i18n.t('errors.SERVICE_UNAVAILABLE', { locale: 'en' })
 * </pre>
 */
public final class CommonMessageKeys {
    
    // ========== Generic Errors (used by all services) ==========
    /**
     * Unknown or unhandled error occurred
     */
    public static final String UNKNOWN_ERROR = "UNKNOWN_ERROR";
    
    /**
     * Internal server error occurred
     */
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    
    /**
     * Service is temporarily unavailable
     */
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    
    // ========== Rate Limiting (used by multiple services) ==========
    /**
     * Rate limit exceeded - too many requests
     */
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    
    // ========== Authentication/Authorization (shared by auth-related services) ==========
    /**
     * Invalid or malformed token
     */
    public static final String INVALID_TOKEN = "INVALID_TOKEN";
    
    /**
     * Token has expired
     */
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    
    /**
     * Invalid authorization header
     */
    public static final String INVALID_AUTHORIZATION_HEADER = "INVALID_AUTHORIZATION_HEADER";
    
    /**
     * Token not in whitelist
     */
    public static final String TOKEN_NOT_IN_WHITELIST = "TOKEN_NOT_IN_WHITELIST";
    
    /**
     * Token has been blacklisted
     */
    public static final String TOKEN_BLACKLISTED = "TOKEN_BLACKLISTED";
    
    /**
     * Invalid token issuer
     */
    public static final String INVALID_TOKEN_ISSUER = "INVALID_TOKEN_ISSUER";
    
    /**
     * Invalid token audience
     */
    public static final String INVALID_TOKEN_AUDIENCE = "INVALID_TOKEN_AUDIENCE";
    
    /**
     * User ID not found in token
     */
    public static final String USER_ID_NOT_FOUND_IN_TOKEN = "USER_ID_NOT_FOUND_IN_TOKEN";
    
    // ========== Validation (shared by all services) ==========
    /**
     * Request validation failed
     */
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    
    /**
     * Resource not found
     */
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    
    private CommonMessageKeys() {
        // Utility class - prevent instantiation
    }
}

