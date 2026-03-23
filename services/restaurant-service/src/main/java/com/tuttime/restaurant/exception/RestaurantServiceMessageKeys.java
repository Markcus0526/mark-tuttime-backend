package com.tuttime.restaurant.exception;

/**
 * Message keys for User Service error internationalization.
 * 
 * <p>Service-specific keys for user-service. All keys use the {@code RESTAURANT.} prefix
 * to avoid conflicts with other services.
 * 
 * <p>For common keys shared across services, use {@link com.tuttime.common.exception.CommonMessageKeys}.
 * 
 * <p><b>Usage:</b>
 * <pre>
 * import com.tuttime.RESTAURANT.exception.UserServiceMessageKeys;
 * import com.tuttime.common.exception.CommonMessageKeys;
 * 
 * // Service-specific key
 * throw new BusinessException("Phone number is required", UserServiceMessageKeys.PHONE_NUMBER_REQUIRED);
 * 
 * // Common key
 * throw new BusinessException("Service unavailable", CommonMessageKeys.SERVICE_UNAVAILABLE);
 * </pre>
 * 
 * <p><b>Frontend Translation:</b>
 * Frontend should use these keys to look up translations:
 * <pre>
 * i18n.t(`errors.${messageKey}`, { locale: 'en' })
 * // e.g., i18n.t('errors.RESTAURANT.PHONE_NUMBER_REQUIRED', { locale: 'en' })
 * </pre>
 */
public final class RestaurantServiceMessageKeys {

    // ========== Authentication - Phone Number ==========
    /**
     * Phone number is required but not provided
     */
    public static final String PHONE_NUMBER_REQUIRED = "RESTAURANT.PHONE_NUMBER_REQUIRED";

    /**
     * Phone number format is invalid
     */
    public static final String INVALID_PHONE_NUMBER_FORMAT = "RESTAURANT.INVALID_PHONE_NUMBER_FORMAT";

    /**
     * Phone number is already registered
     */
    public static final String PHONE_NUMBER_ALREADY_REGISTERED = "RESTAURANT.PHONE_NUMBER_ALREADY_REGISTERED";

    /**
     * Phone number is not registered
     */
    public static final String PHONE_NUMBER_NOT_REGISTERED = "RESTAURANT.PHONE_NUMBER_NOT_REGISTERED";

    // ========== Authentication - SMS Verification ==========
    /**
     * SMS verification code is required
     */
    public static final String SMS_CODE_REQUIRED = "RESTAURANT.SMS_CODE_REQUIRED";

    /**
     * SMS code is invalid or has expired
     */
    public static final String SMS_CODE_INVALID_OR_EXPIRED = "RESTAURANT.SMS_CODE_INVALID_OR_EXPIRED";

    /**
     * SMS code verification failed
     */
    public static final String SMS_CODE_VERIFICATION_FAILED = "RESTAURANT.SMS_CODE_VERIFICATION_FAILED";

    /**
     * SMS send attempts exceeded (rate limit)
     */
    public static final String SMS_SEND_ATTEMPTS_EXCEEDED = "RESTAURANT.SMS_SEND_ATTEMPTS_EXCEEDED";

    /**
     * SMS service is unavailable
     */
    public static final String SMS_SERVICE_UNAVAILABLE = "RESTAURANT.SMS_SERVICE_UNAVAILABLE";

    /**
     * SMS code not found or expired in storage
     */
    public static final String SMS_CODE_NOT_FOUND_OR_EXPIRED = "RESTAURANT.SMS_CODE_NOT_FOUND_OR_EXPIRED";

    /**
     * Failed to send SMS
     */
    public static final String FAILED_TO_SEND_SMS = "RESTAURANT.FAILED_TO_SEND_SMS";

    /**
     * SMS rate limit exceeded (too many requests to phone number)
     */
    public static final String SMS_RATE_LIMIT_EXCEEDED = "RESTAURANT.SMS_RATE_LIMIT_EXCEEDED";

    /**
     * SMS verification service is unavailable
     */
    public static final String SMS_VERIFICATION_SERVICE_UNAVAILABLE = "RESTAURANT.SMS_VERIFICATION_SERVICE_UNAVAILABLE";

    // ========== Authentication - Signup Token ==========
    /**
     * Signup token is required
     */
    public static final String SIGNUP_TOKEN_REQUIRED = "RESTAURANT.SIGNUP_TOKEN_REQUIRED";

    /**
     * Signup token is invalid or expired
     */
    public static final String SIGNUP_TOKEN_INVALID_OR_EXPIRED = "RESTAURANT.SIGNUP_TOKEN_INVALID_OR_EXPIRED";

    /**
     * Signup token missing JTI claim
     */
    public static final String SIGNUP_TOKEN_MISSING_JTI = "RESTAURANT.SIGNUP_TOKEN_MISSING_JTI";

    /**
     * Signup token has expired or been used
     */
    public static final String SIGNUP_TOKEN_EXPIRED_OR_USED = "RESTAURANT.SIGNUP_TOKEN_EXPIRED_OR_USED";

    /**
     * Signup token phone number mismatch
     */
    public static final String SIGNUP_TOKEN_PHONE_MISMATCH = "RESTAURANT.SIGNUP_TOKEN_PHONE_MISMATCH";

    /**
     * Signup token validation service is unavailable
     */
    public static final String SIGNUP_TOKEN_VALIDATION_SERVICE_UNAVAILABLE = "RESTAURANT.SIGNUP_TOKEN_VALIDATION_SERVICE_UNAVAILABLE";

    /**
     * Phone number does not match signup token
     */
    public static final String PHONE_NUMBER_DOES_NOT_MATCH_SIGNUP_TOKEN = "RESTAURANT.PHONE_NUMBER_DOES_NOT_MATCH_SIGNUP_TOKEN";

    // ========== Authentication - Credentials ==========
    /**
     * Invalid phone number or password
     */
    public static final String INVALID_PHONE_NUMBER_OR_PASSWORD = "RESTAURANT.INVALID_PHONE_NUMBER_OR_PASSWORD";

    /**
     * Invalid email or password
     */
    public static final String INVALID_EMAIL_OR_PASSWORD = "RESTAURANT.INVALID_EMAIL_OR_PASSWORD";

    /**
     * Password is required
     */
    public static final String PASSWORD_REQUIRED = "RESTAURANT.PASSWORD_REQUIRED";

    /**
     * Password is too short
     */
    public static final String PASSWORD_TOO_SHORT = "RESTAURANT.PASSWORD_TOO_SHORT";

    /**
     * Password does not meet requirements
     */
    public static final String PASSWORD_DOES_NOT_MEET_REQUIREMENTS = "RESTAURANT.PASSWORD_DOES_NOT_MEET_REQUIREMENTS";

    // ========== Authentication - Refresh Token ==========
    /**
     * Refresh token is required
     */
    public static final String REFRESH_TOKEN_REQUIRED = "RESTAURANT.REFRESH_TOKEN_REQUIRED";

    /**
     * Refresh token is invalid or expired
     */
    public static final String REFRESH_TOKEN_INVALID_OR_EXPIRED = "RESTAURANT.REFRESH_TOKEN_INVALID_OR_EXPIRED";

    /**
     * Refresh token missing JTI claim
     */
    public static final String REFRESH_TOKEN_MISSING_JTI = "RESTAURANT.REFRESH_TOKEN_MISSING_JTI";

    /**
     * Refresh token has been revoked
     */
    public static final String REFRESH_TOKEN_REVOKED = "RESTAURANT.REFRESH_TOKEN_REVOKED";

    /**
     * Refresh token type is invalid
     */
    public static final String REFRESH_TOKEN_INVALID_TYPE = "RESTAURANT.REFRESH_TOKEN_INVALID_TYPE";

    /**
     * Invalid refresh token
     */
    public static final String INVALID_REFRESH_TOKEN = "RESTAURANT.INVALID_REFRESH_TOKEN";

    // ========== Authentication - User Account ==========
    /**
     * User account is inactive
     */
    public static final String USER_ACCOUNT_INACTIVE = "RESTAURANT.USER_ACCOUNT_INACTIVE";

    /**
     * User account is blocked
     */
    public static final String USER_ACCOUNT_BLOCKED = "RESTAURANT.USER_ACCOUNT_BLOCKED";

    /**
     * User not found
     */
    public static final String USER_NOT_FOUND = "RESTAURANT.USER_NOT_FOUND";

    /**
     * Email is required
     */
    public static final String EMAIL_REQUIRED = "RESTAURANT.EMAIL_REQUIRED";

    /**
     * Email is already registered
     */
    public static final String EMAIL_ALREADY_REGISTERED = "RESTAURANT.EMAIL_ALREADY_REGISTERED";

    /**
     * Username is required
     */
    public static final String USERNAME_REQUIRED = "RESTAURANT.USERNAME_REQUIRED";

    /**
     * Username is already taken
     */
    public static final String USERNAME_ALREADY_TAKEN = "RESTAURANT.USERNAME_ALREADY_TAKEN";

    /**
     * Username format is invalid
     */
    public static final String USERNAME_INVALID_FORMAT = "RESTAURANT.USERNAME_INVALID_FORMAT";

    // ========== Device & Anti-Fraud ==========
    /**
     * Device ID is required
     */
    public static final String DEVICE_ID_REQUIRED = "RESTAURANT.DEVICE_ID_REQUIRED";

    /**
     * Device fingerprint is required
     */
    public static final String DEVICE_FINGERPRINT_REQUIRED = "RESTAURANT.DEVICE_FINGERPRINT_REQUIRED";

    /**
     * Device is already registered to another account
     */
    public static final String DEVICE_ALREADY_REGISTERED = "RESTAURANT.DEVICE_ALREADY_REGISTERED";

    /**
     * Maximum device limit reached
     */
    public static final String DEVICE_LIMIT_REACHED = "RESTAURANT.DEVICE_LIMIT_REACHED";

    /**
     * Device not found for user
     */
    public static final String DEVICE_NOT_FOUND = "RESTAURANT.DEVICE_NOT_FOUND";

    /**
     * Multiple accounts detected from same IP address
     */
    public static final String MULTI_ACCOUNT_DETECTED_IP = "RESTAURANT.MULTI_ACCOUNT_DETECTED_IP";

    /**
     * Multiple accounts detected on same device
     */
    public static final String MULTI_ACCOUNT_DETECTED_DEVICE = "RESTAURANT.MULTI_ACCOUNT_DETECTED_DEVICE";

    // ========== Authentication - Email Verification ==========
    /**
     * Email verification code is required
     */
    public static final String EMAIL_CODE_REQUIRED = "RESTAURANT.EMAIL_CODE_REQUIRED";

    /**
     * Email code is invalid or has expired
     */
    public static final String EMAIL_CODE_INVALID_OR_EXPIRED = "RESTAURANT.EMAIL_CODE_INVALID_OR_EXPIRED";

    /**
     * Email code verification failed
     */
    public static final String EMAIL_CODE_VERIFICATION_FAILED = "RESTAURANT.EMAIL_CODE_VERIFICATION_FAILED";

    /**
     * Email send attempts exceeded (rate limit)
     */
    public static final String EMAIL_SEND_ATTEMPTS_EXCEEDED = "RESTAURANT.EMAIL_SEND_ATTEMPTS_EXCEEDED";

    /**
     * Email verification service is unavailable
     */
    public static final String EMAIL_VERIFICATION_SERVICE_UNAVAILABLE = "RESTAURANT.EMAIL_VERIFICATION_SERVICE_UNAVAILABLE";

    /**
     * Failed to send email
     */
    public static final String FAILED_TO_SEND_EMAIL = "RESTAURANT.FAILED_TO_SEND_EMAIL";

    /**
     * Password reset verification code send failed (all channels exhausted)
     */
    public static final String PASSWORD_RESET_VERIFICATION_SEND_FAILED = "RESTAURANT.PASSWORD_RESET_VERIFICATION_SEND_FAILED";

    // ========== Rate Limiting & Brute Force ==========
    /**
     * Too many failed login attempts
     */
    public static final String TOO_MANY_FAILED_ATTEMPTS = "RESTAURANT.TOO_MANY_FAILED_ATTEMPTS";

    private RestaurantServiceMessageKeys() {
        // Utility class - prevent instantiation
    }
}

